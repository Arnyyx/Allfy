// QRScannerScreen.kt
package com.arny.allfy.presentation.ui

import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.arny.allfy.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QRScannerScreen(
    navController: NavController,
    onQRScanned: (String) -> Unit,
    onScanFromGallery: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasFlash by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val scanLineAnimation = rememberInfiniteTransition(label = "scanLine")
    val scanLinePosition by scanLineAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLinePosition"
    )

    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseAnimation.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = Executors.newSingleThreadExecutor()

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider

                    val preview = Preview.Builder().build()
                    val selector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setTargetResolution(android.util.Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalyzer.setAnalyzer(executor) { imageProxy ->
                        processImageProxy(imageProxy, onQRScanned)
                    }

                    try {
                        provider.unbindAll()
                        camera = provider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageAnalyzer
                        )

                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        hasFlash = camera?.cameraInfo?.hasFlashUnit() == true

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val scanAreaSize = size.width * 0.7f
            val scanAreaTop = (size.height - scanAreaSize) / 2
            val scanAreaLeft = (size.width - scanAreaSize) / 2

            // Dark overlay
            drawRect(
                color = QROverlay,
                size = size
            )

            // Clear scan area
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(scanAreaLeft, scanAreaTop),
                size = Size(scanAreaSize, scanAreaSize),
                cornerRadius = CornerRadius(16.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // Scan area border with pulse effect
            drawRoundRect(
                color = Color.White.copy(alpha = pulseAlpha),
                topLeft = Offset(scanAreaLeft, scanAreaTop),
                size = Size(scanAreaSize, scanAreaSize),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )

            // Corner indicators
            val cornerLength = 30.dp.toPx()
            val cornerWidth = 4.dp.toPx()
            val cornerColor = QRCorner

            // Top-left corner
            drawRoundRect(
                color = cornerColor,
                topLeft = Offset(scanAreaLeft - cornerWidth / 2, scanAreaTop - cornerWidth / 2),
                size = Size(cornerLength, cornerWidth),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
            drawRoundRect(
                color = cornerColor,
                topLeft = Offset(scanAreaLeft - cornerWidth / 2, scanAreaTop - cornerWidth / 2),
                size = Size(cornerWidth, cornerLength),
                cornerRadius = CornerRadius(2.dp.toPx())
            )

            // Top-right corner
            drawRoundRect(
                color = cornerColor,
                topLeft = Offset(
                    scanAreaLeft + scanAreaSize - cornerLength + cornerWidth / 2,
                    scanAreaTop - cornerWidth / 2
                ),
                size = Size(cornerLength, cornerWidth),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
            drawRoundRect(
                color = cornerColor,
                topLeft = Offset(
                    scanAreaLeft + scanAreaSize - cornerWidth / 2,
                    scanAreaTop - cornerWidth / 2
                ),
                size = Size(cornerWidth, cornerLength),
                cornerRadius = CornerRadius(2.dp.toPx())
            )

            // Bottom-left corner
            drawRoundRect(
                color = cornerColor,
                topLeft = Offset(
                    scanAreaLeft - cornerWidth / 2,
                    scanAreaTop + scanAreaSize - cornerWidth / 2
                ),
                size = Size(cornerLength, cornerWidth),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
            drawRoundRect(
                color = cornerColor,
                topLeft = Offset(
                    scanAreaLeft - cornerWidth / 2,
                    scanAreaTop + scanAreaSize - cornerLength + cornerWidth / 2
                ),
                size = Size(cornerWidth, cornerLength),
                cornerRadius = CornerRadius(2.dp.toPx())
            )

            // Bottom-right corner
            drawRoundRect(
                color = cornerColor,
                topLeft = Offset(
                    scanAreaLeft + scanAreaSize - cornerLength + cornerWidth / 2,
                    scanAreaTop + scanAreaSize - cornerWidth / 2
                ),
                size = Size(cornerLength, cornerWidth),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
            drawRoundRect(
                color = cornerColor,
                topLeft = Offset(
                    scanAreaLeft + scanAreaSize - cornerWidth / 2,
                    scanAreaTop + scanAreaSize - cornerLength + cornerWidth / 2
                ),
                size = Size(cornerWidth, cornerLength),
                cornerRadius = CornerRadius(2.dp.toPx())
            )

            // Animated scan line
            val scanLineY = scanAreaTop + (scanAreaSize * scanLinePosition)
            drawRoundRect(
                color = QRScanLine,
                topLeft = Offset(scanAreaLeft + 20.dp.toPx(), scanLineY),
                size = Size(scanAreaSize - 40.dp.toPx(), 2.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = "Scan QR Code",
                style = MaterialTheme.typography.titleMedium,
                color = QRTextPrimary,
                fontWeight = FontWeight.Bold
            )

            if (hasFlash) {
                IconButton(
                    onClick = {
                        camera?.cameraControl?.enableTorch(!isFlashOn)
                        isFlashOn = !isFlashOn
                    },
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Instructions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Position QR code within the frame",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "The code will be scanned automatically",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Gallery button
            OutlinedButton(
                onClick = onScanFromGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Choose from Gallery",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun QRCodeDisplay(
    qrCodeBitmap: android.graphics.Bitmap,
    username: String,
    onDismiss: () -> Unit,
    onSaveToGallery: () -> Unit
) {
    val rotationAnimation = rememberInfiniteTransition(label = "rotation")
    val rotation by rotationAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile info
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // QR Code with animated border
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    // Animated gradient border
                    Canvas(
                        modifier = Modifier.size(240.dp)
                    ) {
                        drawCircle(
                            brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                colors = listOf(
                                    QRPrimary,
                                    QRSecondary,
                                    QRTertiary,
                                    QRPrimary
                                ),
                                center = center
                            ),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // QR Code container
                    Surface(
                        modifier = Modifier.size(220.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = qrCodeBitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.size(180.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Scan this code to visit my profile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QRTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveToGallery,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    onQRScanned: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        val scanner = BarcodeScanning.getClient(options)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { qrContent ->
                        onQRScanned(qrContent)
                        return@addOnSuccessListener
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}