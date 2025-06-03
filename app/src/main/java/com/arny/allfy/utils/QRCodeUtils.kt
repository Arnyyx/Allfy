package com.arny.allfy.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

suspend fun decodeQRFromImage(context: Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            MultiFormatReader().decode(binaryBitmap).text
        } catch (e: Exception) {
            Log.e("QRDecode", "Error decoding QR from image", e)
            null
        }
    }
}

fun saveQRCodeToGallery(context: Context, bitmap: Bitmap, onComplete: (Boolean) -> Unit) {
    try {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "QR_Code_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Allfy")
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                onComplete(true)
            } ?: onComplete(false)
        } ?: onComplete(false)
    } catch (e: Exception) {
        Log.e("SaveQR", "Error saving QR code", e)
        onComplete(false)
    }
}
