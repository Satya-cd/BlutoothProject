package com.example.myarchitechture

// Import necessary Android, Compose, and ViewModel classes
import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.myarchitechture.ui.theme.BluetoothDeviceGrid
import com.example.myarchitechture.ui.theme.MyArchitechtureTheme
import com.example.myarchitechture.viewmodel.BluetoothViewModel
import com.example.myarchitechture.ui.theme.MyArchitechtureTheme
import com.example.myarchitechture.ui.theme.BluetoothPermissionHandler
class MainActivity : ComponentActivity() {

    // Create an instance of the BluetoothViewModel
    private val bluetoothViewModel: BluetoothViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Define required permissions based on Android version
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31) and above need new Bluetooth permissions
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            // Older Android versions use legacy permissions
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        // Launcher to request multiple permissions at once
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsResult ->
            // Check if all requested permissions were granted
            val granted = permissionsResult.values.all { it == true }
            try {
                // Inform ViewModel about permission result
                bluetoothViewModel.onPermissionResult(granted, this)

                if (granted) {
                    // If granted, check Bluetooth status (enabled/disabled)
                    bluetoothViewModel.checkBluetoothStatus(this)
                }
            } catch (e: SecurityException) {
                e.printStackTrace()  // Catch any unexpected permission issues
            }
        }

        // Set up the Compose UId
        setContent {
            MyArchitechtureTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Observe the list of Bluetooth devices from the ViewModel
                    val devices by bluetoothViewModel.bluetoothDevices.collectAsState()
                    BluetoothDeviceGrid(devices, bluetoothViewModel)


                }
            }
        }

        // Immediately request permissions when activity starts
        permissionLauncher.launch(permissions)
    }
}

