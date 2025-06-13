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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
        DurationOption("5 mins", 300L),
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

    val currentUser = when (val state = userState.currentUserState) {
        is Response.Success -> state.data
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Loading progress indicator at top
        if (isUploading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(10f),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        // Main content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopHeader(
                currentUser = currentUser,
                selectedUri = selectedUri,
                isUploading = isUploading,
                onBackClick = { navController.popBackStack() },
                onShareClick = {
                    selectedUri?.let { uri ->
                        currentUser?.userId?.let { id ->
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
                }
            )

            // Content area - more space for media
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (selectedUri != null) {
                    MediaPreview(
                        uri = selectedUri!!,
                        mediaType = selectedMediaType ?: "",
                        exoPlayer = exoPlayer
                    )
                } else {
                    EmptyState()
                }
            }

            // Bottom controls - compact layout
            BottomControls(
                selectedUri = selectedUri,
                selectedDuration = selectedDuration,
                isDurationMenuExpanded = isDurationMenuExpanded,
                durationOptions = durationOptions,
                onDurationExpandedChange = { isDurationMenuExpanded = it },
                onDurationSelected = { selectedDuration = it },
                onCameraClick = {
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
                            errorMessage = "Failed to create image file for camera"
                        }
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onImagePickerClick = { imagePickerLauncher.launch("image/*") },
                onVideoPickerClick = { videoPickerLauncher.launch("video/*") }
            )
        }
    }
}

@Composable
private fun TopHeader(
    currentUser: com.arny.allfy.domain.model.User?,
    selectedUri: Uri?,
    isUploading: Boolean,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .zIndex(5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // User info
        currentUser?.let { user ->
            // Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f))
            ) {
                Image(
                    painter = rememberAsyncImagePainter(user.imageUrl),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Username
            Text(
                text = user.username,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Share button - only show when media is selected
        if (selectedUri != null) {
            Button(
                onClick = onShareClick,
                enabled = !isUploading && currentUser != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                if (isUploading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sharing...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        text = "Share",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaPreview(
    uri: Uri,
    mediaType: String,
    exoPlayer: ExoPlayer
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        when (mediaType) {
            "image" -> {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.FillHeight
                )
            }

            "video" -> {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = false
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Color.White.copy(alpha = 0.05f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White.copy(alpha = 0.4f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Add to your story",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Share photos and videos that disappear after 24 hours",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomControls(
    selectedUri: Uri?,
    selectedDuration: Long,
    isDurationMenuExpanded: Boolean,
    durationOptions: List<DurationOption>,
    onDurationExpandedChange: (Boolean) -> Unit,
    onDurationSelected: (Long) -> Unit,
    onCameraClick: () -> Unit,
    onImagePickerClick: () -> Unit,
    onVideoPickerClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.4f),
                        Color.Black.copy(alpha = 0.8f),
                        Color.Black
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(getMediaOptions()) { option ->
                    MediaOptionItem(
                        option = option,
                        onClick = {
                            when (option.type) {
                                "camera" -> onCameraClick()
                                "gallery_image" -> onImagePickerClick()
                                "gallery_video" -> onVideoPickerClick()
                            }
                        }
                    )
                }
            }

            if (selectedUri != null) {
                ExposedDropdownMenuBox(
                    expanded = isDurationMenuExpanded,
                    onExpandedChange = onDurationExpandedChange,
                    modifier = Modifier.weight(0.7f)
                ) {
                    OutlinedTextField(
                        value = durationOptions.find { it.seconds == selectedDuration }?.label
                            ?: "24h",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = if (isDurationMenuExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Toggle duration menu",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color.White
                        ),
                        modifier = Modifier
                            .menuAnchor(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )

                    ExposedDropdownMenu(
                        expanded = isDurationMenuExpanded,
                        onDismissRequest = { onDurationExpandedChange(false) },
                        modifier = Modifier.background(
                            Color.Black.copy(alpha = 0.95f),
                            RoundedCornerShape(8.dp)
                        )
                    ) {
                        durationOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                },
                                onClick = {
                                    onDurationSelected(option.seconds)
                                    onDurationExpandedChange(false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaOptionItem(
    option: MediaOption,
    onClick: () -> Unit
) {
    val size = 52.dp
    val iconSize = 22.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = option.type,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

private data class MediaOption(
    val type: String,
    val icon: ImageVector
)

private data class DurationOption(
    val label: String,
    val seconds: Long
)

private fun getMediaOptions(): List<MediaOption> = listOf(
    MediaOption("camera", Icons.Default.PhotoCamera),
    MediaOption("gallery_image", Icons.Default.Photo),
    MediaOption("gallery_video", Icons.Default.Videocam)
)