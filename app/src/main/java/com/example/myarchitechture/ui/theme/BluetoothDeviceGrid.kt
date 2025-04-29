package com.example.myarchitechture.ui.theme

// Import necessary Android and Compose libraries
import android.widget.Toast
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.myarchitechture.viewmodel.BluetoothViewModel

// Main Composable function to display a grid of Bluetooth devices
@Composable
fun BluetoothDeviceGrid(devices: List<BluetoothDevice>, bluetoothViewModel: BluetoothViewModel) {
    val context = LocalContext.current

    // Get the Bluetooth adapter
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    val isBluetoothEnabled = bluetoothAdapter?.isEnabled ?: false

    // Launcher for turning on Bluetooth via system intent (Android 13+)
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // If Bluetooth is enabled successfully, start scanning for devices
            bluetoothViewModel.startScanning(context)
        } else {
            // If user cancels or fails, show a Toast
            Toast.makeText(context, "Failed to enable Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    // Lazy grid to display the devices (2 columns)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        if (!isBluetoothEnabled) {
            // If Bluetooth is OFF, show a card asking to turn it ON
            items(1) {
                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Bluetooth is Off",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please turn it ON to see nearby devices.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Button to turn ON Bluetooth
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    // For Android 13+, open system dialog to request enabling Bluetooth
                                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                    enableBluetoothLauncher.launch(enableBtIntent)
                                } else {
                                    // For older Android versions, try to enable programmatically
                                    bluetoothViewModel.turnOnBluetoothDirectly(context)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Turn On Bluetooth")
                        }
                    }
                }
            }
        } else {
            // If Bluetooth is ON, show the list of scanned devices
            items(devices) { device ->
                // Check if we have permission to access device name
                val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        )

                // Get device name safely with permission check
                val deviceName = try {
                    if (hasPermission) device.name ?: "Unknown"
                    else "Permission Needed"
                } catch (e: SecurityException) {
                    "Permission Needed"
                }

                // Display each Bluetooth device inside a Card
                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Name: $deviceName")
                        Text("Address: ${device.address}")
                    }
                }
            }
        }
    }
}



