package com.example.myarchitechture.ui.theme


import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun BluetoothPermissionHandler(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var permissionDeniedCount by remember { mutableStateOf(0) }
    var showSettingsUI by remember { mutableStateOf(false) }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted) {
            showSettingsUI = false
            permissionDeniedCount = 0
        } else {
            permissionDeniedCount++
            if (permissionDeniedCount >= 2) {
                showSettingsUI = true
            }
        }
    }

    val allPermissionsGranted = permissions.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    when {
        allPermissionsGranted -> {
            content()
        }
        showSettingsUI -> {
            PermissionSettingsUI {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }
        else -> {
            SideEffect {
                permissionLauncher.launch(permissions)
            }
        }
    }
}

@Composable
fun PermissionSettingsUI(onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Nearby Share permission is required")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onClick) {
                    Text("Open Settings")
                }
            }
        }
    }
}