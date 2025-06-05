package com.arny.allfy.utils

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.BarcodeEncoder

fun generateQRCode(content: String, size: Int = 512): Bitmap? {
    return try {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size)
        BarcodeEncoder().createBitmap(bitMatrix)
    } catch (e: Exception) {
        null
    }
}