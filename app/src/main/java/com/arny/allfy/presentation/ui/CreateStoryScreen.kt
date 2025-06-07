package com.arny.allfy.presentation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.arny.allfy.domain.model.Story
import com.arny.allfy.presentation.common.Dialog
import com.arny.allfy.presentation.viewmodel.StoryViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    navController: NavController,
    storyViewModel: StoryViewModel,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf<String?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    val storyState by storyViewModel.storyState.collectAsState()
    val userState by userViewModel.userState.collectAsState()
    var isUploading by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedDuration by remember { mutableLongStateOf(86400L) }
    var isDurationMenuExpanded by remember { mutableStateOf(false) }

    val durationOptions = listOf(
        DurationOption("5 minutes", 30L),
        DurationOption("1 hour", 3600L),
        DurationOption("6 hours", 21600L),
        DurationOption("12 hours", 43200L),
        DurationOption("24 hours", 86400L)
    )

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(selectedUri, selectedMediaType) {
        if (selectedMediaType == "video" && selectedUri != null) {
            val mediaItem = MediaItem.fromUri(selectedUri!!)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } else {
            exoPlayer.stop()
        }
    }

    val userId = when (val state = userState.currentUserState) {
        is Response.Success -> state.data.userId
        else -> null
    }

    val createImageFile = remember {
        {
            try {
                val storageDir = File(context.cacheDir, "images")
                if (!storageDir.exists()) storageDir.mkdirs()

                val imageFile = File.createTempFile(
                    "JPEG_${System.currentTimeMillis()}_",
                    ".jpg",
                    storageDir
                )
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    imageFile
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                selectedUri = uri
                selectedMediaType = "image"
            }
        } else {
            cameraImageUri = null
        }
    }

    // Permission launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createImageFile()
            if (uri != null) {
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            } else {
                errorMessage = "Failed to create image file for camera"
            }
        } else {
            showPermissionDialog = true
        }
    }

    // Media picker launchers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            selectedMediaType = "image"
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            selectedMediaType = "video"
        }
    }

    // Handle upload story state
    LaunchedEffect(storyState.uploadStoryState) {
        when (val uploadState = storyState.uploadStoryState) {
            is Response.Loading -> {
                isUploading = true
            }

            is Response.Success -> {
                isUploading = false
                Toast.makeText(context, "Story uploaded successfully!", Toast.LENGTH_SHORT).show()
                storyViewModel.resetUploadStoryState()
                navController.popBackStack()
            }

            is Response.Error -> {
                isUploading = false
                errorMessage = "Failed to upload story: ${uploadState.message}"
                storyViewModel.resetUploadStoryState()
            }

            else -> {
                isUploading = false
            }
        }
    }

    // Permission dialog
    if (showPermissionDialog) {
        Dialog(
            title = "Camera Permission Required",
            message = "The app needs camera permission to take photos for your story.",
            confirmText = "OK",
            dismissText = "",
            onConfirm = { showPermissionDialog = false },
            onDismiss = { showPermissionDialog = false }
        )
    }

    // Error dialog
    errorMessage?.let { message ->
        Dialog(
            title = "Error",
            message = message,
            confirmText = "OK",
            dismissText = "",
            onConfirm = { errorMessage = null },
            onDismiss = { errorMessage = null }
        )
    }

    Scaffold(
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Create Story",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(
                        onClick = { /* Settings */ }
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedUri != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f / 16f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            when (selectedMediaType) {
                                "image" -> {
                                    Image(
                                        painter = rememberAsyncImagePainter(selectedUri),
                                        contentDescription = "Selected image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                "video" -> {
                                    AndroidView(
                                        factory = { context ->
                                            PlayerView(context).apply {
                                                player = exoPlayer
                                                useController = false // Hide controls
                                                layoutParams = android.view.ViewGroup.LayoutParams(
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty state
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Select a photo or video\nto create your story",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Bottom controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    // Duration selection
                    ExposedDropdownMenuBox(
                        expanded = isDurationMenuExpanded,
                        onExpandedChange = { isDurationMenuExpanded = !isDurationMenuExpanded }
                    ) {
                        TextField(
                            value = durationOptions.find { it.seconds == selectedDuration }?.label
                                ?: "Select Duration",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = if (isDurationMenuExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Toggle duration menu",
                                    tint = Color.White
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.White.copy(alpha = 0.6f),
                                focusedContainerColor = Color.White.copy(alpha = 0.1f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = isDurationMenuExpanded,
                            onDismissRequest = { isDurationMenuExpanded = false },
                            modifier = Modifier.background(Color.Black)
                        ) {
                            durationOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {
                                        selectedDuration = option.seconds
                                        isDurationMenuExpanded = false
                                    },
                                    modifier = Modifier.background(Color.Black)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Media type selection
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(getMediaOptions()) { option ->
                            MediaOptionItem(
                                option = option,
                                onClick = {
                                    when (option.type) {
                                        "camera" -> {
                                            if (ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.CAMERA
                                                ) == PackageManager.PERMISSION_GRANTED
                                            ) {
                                                val uri = createImageFile()
                                                if (uri != null) {
                                                    cameraImageUri = uri
                                                    cameraLauncher.launch(uri)
                                                } else {
                                                    errorMessage =
                                                        "Failed to create image file for camera"
                                                }
                                            } else {
                                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        }

                                        "gallery_image" -> imagePickerLauncher.launch("image/*")
                                        "gallery_video" -> videoPickerLauncher.launch("video/*")
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Upload button
                    Button(
                        onClick = {
                            selectedUri?.let { uri ->
                                userId?.let { id ->
                                    val story = Story(
                                        userID = id,
                                        imageDuration = if (selectedMediaType == "image") 5000L else null,
                                        maxVideoDuration = if (selectedMediaType == "video") 30000L else null,
                                        duration = selectedDuration
                                    )
                                    storyViewModel.uploadStory(story, uri)
                                } ?: run {
                                    errorMessage = "Unable to get user information"
                                }
                            }
                        },
                        enabled = selectedUri != null && !isUploading && userId != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        if (isUploading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Uploading...",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                text = "Share to Story",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Loading overlay
            if (isUploading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
private fun MediaOptionItem(
    option: MediaOption,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = option.label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

private data class MediaOption(
    val type: String,
    val label: String,
    val icon: ImageVector
)

private data class DurationOption(
    val label: String,
    val seconds: Long
)

private fun getMediaOptions(): List<MediaOption> = listOf(
    MediaOption("camera", "Camera", Icons.Default.PhotoCamera),
    MediaOption("gallery_image", "Photo", Icons.Default.Photo),
    MediaOption("gallery_video", "Video", Icons.Default.Videocam)
)