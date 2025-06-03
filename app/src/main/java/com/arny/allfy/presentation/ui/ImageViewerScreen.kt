package com.arny.allfy.presentation.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
fun ImageViewerScreen(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isSaving by remember { mutableStateOf(false) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            saveImageToGallery(context, imageUrl) { success ->
                isSaving = false
                Toast.makeText(
                    context,
                    if (success) "Image saved to gallery" else "Failed to save image",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            isSaving = false
            Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.9f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Full-screen Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f),
                        contentScale = ContentScale.Fit
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    AnimatedVisibility(visible = isSaving) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = {
                            isSaving = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                saveImageToGallery(context, imageUrl) { success ->
                                    isSaving = false
                                    Toast.makeText(
                                        context,
                                        if (success) "Image saved to gallery" else "Failed to save image",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    saveImageToGallery(context, imageUrl) { success ->
                                        isSaving = false
                                        Toast.makeText(
                                            context,
                                            if (success) "Image saved to gallery" else "Failed to save image",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Save",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save to Gallery")
                    }
                }
            }
        }
    }
}

private fun saveImageToGallery(context: Context, imageUrl: String, onComplete: (Boolean) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "Image_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Allfy")
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .build()
                    val drawable = withContext(Dispatchers.IO) {
                        context.imageLoader.execute(request).drawable
                    }
                    if (drawable is BitmapDrawable) {
                        drawable.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        withContext(Dispatchers.Main) {
                            onComplete(true)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onComplete(false)
                        }
                    }
                } ?: withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            } ?: withContext(Dispatchers.Main) {
                onComplete(false)
            }
        } catch (e: IOException) {
            withContext(Dispatchers.Main) {
                onComplete(false)
            }
        }
    }
}