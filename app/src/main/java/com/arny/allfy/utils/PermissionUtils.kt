package com.arny.allfy.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavController

@Composable
fun CameraPermissionLauncher(
    navController: NavController,
    onPermissionDenied: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController.navigate(Screen.QRScannerScreen)
        } else {
            onPermissionDenied()
        }
    }
    return {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            navController.navigate(Screen.QRScannerScreen)
        } else {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
}

@Composable
fun StoragePermissionLauncher(
    context: Context,
    qrCodeBitmap: Bitmap?,
    onPermissionDenied: () -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            qrCodeBitmap?.let { bitmap ->
                saveQRCodeToGallery(context, bitmap) { success ->
                    Toast.makeText(
                        context,
                        if (success) "QR Code saved to gallery" else "Failed to save QR Code",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            onPermissionDenied()
        }
    }
    return {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            qrCodeBitmap?.let { bitmap ->
                saveQRCodeToGallery(context, bitmap) { success ->
                    Toast.makeText(
                        context,
                        if (success) "QR Code saved to gallery" else "Failed to save QR Code",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}