package com.example.myarchitechture.viewmodel

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ViewModel to manage Bluetooth scanning and permissions
class BluetoothViewModel : ViewModel() {

    // Holds the list of discovered Bluetooth devices (observable using StateFlow)
    private val _bluetoothDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val bluetoothDevices: StateFlow<List<BluetoothDevice>> = _bluetoothDevices

    // To track how many times the user denied the permission
    private var denyCount = 0

    // Called when permission result is received
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun onPermissionResult(granted: Boolean, context: Context) {
        if (granted) {
            // If permission granted, start scanning
            scanNearbyDevices(context)
        } else {
            // If denied, increase the deny counter
            denyCount++
            // If denied 3 or more times, open app settings for manual permission grant
            if (denyCount >= 3) openAppSettings(context)
        }
    }

    // Scan nearby Bluetooth devices
    private fun scanNearbyDevices(context: Context) {
        // Check if the app has Bluetooth scan permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val discoveredDevices = mutableListOf<BluetoothDevice>()

            // BroadcastReceiver to listen for found Bluetooth devices
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val device: BluetoothDevice? =
                        intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        discoveredDevices.add(it)
                        _bluetoothDevices.value = discoveredDevices.toList()
                    }
                }
            }

            // Register the BroadcastReceiver to listen for ACTION_FOUND intents
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            context.registerReceiver(receiver, filter)

            // Start Bluetooth device discovery
            adapter?.startDiscovery()
        } else {
            // If no permission, show a toast message
            Toast.makeText(context, "Bluetooth Scan Permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    // Open the app settings screen so the user can manually grant permissions
    private fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    // Check if Bluetooth is available and enabled
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun checkBluetoothStatus(context: Context) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            Toast.makeText(context, "No Bluetooth adapter found", Toast.LENGTH_SHORT).show()
        } else {
            if (!adapter.isEnabled) {
                Toast.makeText(context, "Turn on Bluetooth Please", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Turn on Bluetooth directly (works only for Android versions < 13)
    fun turnOnBluetoothDirectly(context: Context) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null && !adapter.isEnabled) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) { // Android 13 is TIRAMISU
                try {
                    adapter.enable() // Attempt to enable Bluetooth programmatically
                    Toast.makeText(context, "Turning on Bluetooth...", Toast.LENGTH_SHORT).show()

                    // Delay to check if Bluetooth got enabled
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (adapter.isEnabled) {
                            scanNearbyDevices(context)
                        } else {
                            Toast.makeText(context, "Failed to turn on Bluetooth", Toast.LENGTH_SHORT).show()
                        }
                    }, 3000) // 3 seconds delay
                } catch (e: SecurityException) {
                    Toast.makeText(context, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Public function to start scanning manually from outside the ViewModel
    fun startScanning(context: Context) {
        scanNearbyDevices(context)
    }
} 