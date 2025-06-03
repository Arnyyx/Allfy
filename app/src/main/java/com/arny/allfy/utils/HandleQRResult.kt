package com.arny.allfy.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.navigation.NavController

fun handleQRResult(qrContent: String, navController: NavController, context: Context) {
    val uri = qrContent.toUri()
    if (uri.scheme == "allfy" && uri.host == "profile") {
        val scannedUserId = uri.pathSegments.firstOrNull()
        if (scannedUserId != null) {
            navController.navigate(Screen.ProfileScreen(userId = scannedUserId))
            Log.d("QRCodeScan", "Navigating to profile: $scannedUserId")
        } else {
            Toast.makeText(context, "Invalid QR code", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Invalid QR code format", Toast.LENGTH_SHORT).show()
    }
}