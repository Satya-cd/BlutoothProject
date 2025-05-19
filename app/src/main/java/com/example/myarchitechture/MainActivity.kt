package com.example.myarchitechture

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.myarchitechture.ui.theme.BluetoothDeviceGrid
import com.example.myarchitechture.ui.theme.MyArchitechtureTheme
import com.example.myarchitechture.viewmodel.BluetoothViewModel

class MainActivity : ComponentActivity() {

    private val bluetoothViewModel: BluetoothViewModel by viewModels()

    // Counters to track number of times permission denied
    private var bluetoothDenyCount = 0
    private var locationDenyCount = 0

    // UI State: Only one dialog should show at a time
    private val showDevices = mutableStateOf(false)
    private val showBluetoothDialog = mutableStateOf(false)
    private val showBluetoothSettingsDialog = mutableStateOf(false)
    private val showLocationDialog = mutableStateOf(false)
    private val showLocationSettingsDialog = mutableStateOf(false)
    private val showAllPermissionsDialog = mutableStateOf(false)

    // Required permissions depending on Android version
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // Launcher to request permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->

        // Check if Bluetooth permissions are granted
        val isBluetoothGranted = results.entries.filter {
            it.key.contains("BLUETOOTH")
        }.all { it.value }

        // Check if location permission is granted
        val isLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // Step 1: Reset all dialog states to avoid overlapping
        resetDialogs()

        // Step 2: Logic to show correct dialog
        when {
            isBluetoothGranted && isLocationGranted -> {
                showDevices.value = true
                bluetoothDenyCount = 0
                locationDenyCount = 0
            }

            !isBluetoothGranted && !isLocationGranted -> {
                bluetoothDenyCount++
                locationDenyCount++
                showAllPermissionsDialog.value = true
            }

            !isBluetoothGranted -> {
                bluetoothDenyCount++
                if (bluetoothDenyCount == 1) {
                    showBluetoothDialog.value = true
                } else {
                    showBluetoothSettingsDialog.value = true
                }
            }

            !isLocationGranted -> {
                locationDenyCount++
                if (locationDenyCount == 1) {
                    showLocationDialog.value = true
                } else {
                    showLocationSettingsDialog.value = true
                }
            }
        }
    }

    // Launcher to open settings screen
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // After returning from settings, re-check permissions
        checkPermissionsManually()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Trigger permission check on app start
        checkPermissionsManually()

        setContent {
            MyArchitechtureTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val devices by bluetoothViewModel.bluetoothDevices.collectAsState()

                    // Show devices grid
                    if (showDevices.value) {
                        BluetoothDeviceGrid(devices, bluetoothViewModel)
                    }

                    // Show only one dialog at a time based on state
                    when {
                        showBluetoothDialog.value -> ShowBluetoothDialog()
                        showBluetoothSettingsDialog.value -> ShowBluetoothSettingsDialog()
                        showLocationDialog.value -> ShowLocationDialog()
                        showLocationSettingsDialog.value -> ShowLocationSettingsDialog()
                        showAllPermissionsDialog.value -> ShowAllPermissionsDialog()
                    }
                }
            }
        }
    }

    // Manually check if all permissions are already granted
    private fun checkPermissionsManually() {
        val isGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED

        }

        if (isGranted) {
            showDevices.value = true
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    // Reset all dialog states to false (hide all)
    private fun resetDialogs() {
        showBluetoothDialog.value = false
        showBluetoothSettingsDialog.value = false
        showLocationDialog.value = false
        showLocationSettingsDialog.value = false
        showAllPermissionsDialog.value = false
    }

    // ----------------------------
    // Dialog Composable Section
    // ----------------------------

    @Composable
    fun ShowBluetoothDialog() {
        Dialog(onDismissRequest = {}) {
            Card(modifier = Modifier.size(300.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Bluetooth Permission Needed", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("We need Bluetooth permission to scan nearby devices.")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        resetDialogs()
                        permissionLauncher.launch(permissions)
                    }) {
                        Text("Allow")
                    }
                }
            }
        }
    }

    @Composable
    fun ShowBluetoothSettingsDialog() {
        val context = LocalContext.current
        Dialog(onDismissRequest = {}) {
            Card(modifier = Modifier.size(300.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Permission Denied", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Please enable Bluetooth permission from Settings.")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        settingsLauncher.launch(intent)
                    }) {
                        Text("Go to Settings")
                    }
                }
            }
        }
    }

    @Composable
    fun ShowLocationDialog() {
        Dialog(onDismissRequest = {}) {
            Card(modifier = Modifier.size(300.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Location Permission Needed", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("To discover nearby Bluetooth devices, location access is required.")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        resetDialogs()
                        permissionLauncher.launch(permissions)
                    }) {
                        Text("Allow")
                    }
                }
            }
        }
    }

    @Composable
    fun ShowLocationSettingsDialog() {
        val context = LocalContext.current
        Dialog(onDismissRequest = {}) {
            Card(modifier = Modifier.size(300.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Location Permission Denied", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Please enable location permission from Settings.")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        settingsLauncher.launch(intent)
                    }) {
                        Text("Go to Settings")
                    }
                }
            }
        }
    }

    @Composable
    fun ShowAllPermissionsDialog() {
        Dialog(onDismissRequest = {}) {
            Card(modifier = Modifier.size(300.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("All Permissions Required", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Bluetooth and Location permissions are required to proceed.")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        resetDialogs()
                        permissionLauncher.launch(permissions)
                    }) {
                        Text("Allow All")
                    }
                }
            }
        }
    }
}







