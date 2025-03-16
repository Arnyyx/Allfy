package com.arny.allfy.presentation.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.Toast
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    navHostController: NavHostController,
    postViewModel: PostViewModel,
    userViewModel: UserViewModel
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val uploadPostState by postViewModel.uploadPostSate.collectAsState()

    var captionText by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImageUris = uris
    }
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { selectedImageUris.size }
    )

    val isLoading = currentUser is Response.Loading || uploadPostState is Response.Loading

    Scaffold(
        topBar = {
            Column {
                CreatePostTopBar(
                    isLoading = isLoading,
                    onCancelClick = { navHostController.popBackStack() },
                    onShareClick = {
                        if (selectedImageUris.isNotEmpty() && currentUser is Response.Success) {
                            val user = (currentUser as Response.Success<User>).data
                            val post = Post(
                                postOwnerID = user.userId,
                                postOwnerUsername = user.username,
                                postOwnerImageUrl = user.imageUrl,
                                caption = captionText,
                                mediaItems = emptyList()
                            )
                            postViewModel.uploadPost(post, selectedImageUris)
                        }
                    },
                    isShareEnabled = selectedImageUris.isNotEmpty()
                )
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            when (currentUser) {
                is Response.Success -> {
                    ImagePickerSection(
                        selectedImageUris = selectedImageUris,
                        pagerState = pagerState,
                        onImagePick = { launcher.launch("image/*;video/*") }
                    )

                    CaptionInput(
                        captionText = captionText,
                        onCaptionChange = { captionText = it }
                    )
                    PostOptions()
                    UploadStateHandler(
                        uploadResponse = uploadPostState,
                        onSuccess = { navHostController.popBackStack() }
                    )
                }

                is Response.Error -> ErrorState(
                    message = (currentUser as Response.Error).message,
                    onRetry = { userViewModel.getCurrentUser() }
                )

                else -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostTopBar(
    isLoading: Boolean,
    onCancelClick: () -> Unit,
    onShareClick: () -> Unit,
    isShareEnabled: Boolean
) {
    TopAppBar(
        title = {
            Text(
                text = "New Post",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onCancelClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            TextButton(
                onClick = onShareClick,
                enabled = isShareEnabled && !isLoading
            ) {
                Text(
                    text = "Share",
                    fontWeight = FontWeight.Bold,
                    color = if (isShareEnabled && !isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.5f
                    )
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun ImagePickerSection(
    selectedImageUris: List<Uri>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onImagePick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .clickable(onClick = onImagePick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = selectedImageUris.isNotEmpty(),
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val uri = selectedImageUris[page]
                val mimeType = context.contentResolver.getType(uri)

                if (mimeType?.startsWith("image/") == true) {
                    AsyncImageWithPlaceholder(
                        imageUrl = uri.toString(),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else if (mimeType?.startsWith("video/") == true) {
                    VideoPlayer(uri = uri, modifier = Modifier.fillMaxSize())
                }
            }
        }
        if (selectedImageUris.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.placehoder_image),
                    contentDescription = "Placeholder Image",
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap to select images or videos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else if (selectedImageUris.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1}/${selectedImageUris.size}",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun VideoPlayer(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun AsyncImageWithPlaceholder(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .placeholder(R.drawable.placehoder_image)
            .error(R.drawable.placehoder_image)
            .build()
    )
    val imageSize = painter.intrinsicSize
    val maxWidth = 500.dp
    val aspectRatio = if (imageSize.height > 0) imageSize.width / imageSize.height else 1f
    val calculatedWidth = minOf(maxWidth, imageSize.width.dp)
    val calculatedHeight = (calculatedWidth / aspectRatio).coerceAtMost(500.dp)

    Image(
        painter = painter,
        contentDescription = "Selected Image",
        modifier = modifier
            .width(calculatedWidth)
            .height(calculatedHeight),
        contentScale = contentScale
    )
}

@Composable
private fun CaptionInput(
    captionText: String,
    onCaptionChange: (String) -> Unit
) {
    TextField(
        value = captionText,
        onValueChange = onCaptionChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Write a caption...") },
        maxLines = 5,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun PostOptions() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = { /* TODO: Implement tagging people */ }) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Tag People",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = { /* TODO: Implement adding location */ }) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Add Location",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = { /* TODO: Implement advanced settings */ }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More Options",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun UploadStateHandler(
    uploadResponse: Response<Boolean>,
    onSuccess: () -> Unit
) {
    when (uploadResponse) {
        is Response.Error -> {
            Text(
                text = uploadResponse.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(16.dp)
            )
        }

        is Response.Success -> {
            Toast("Upload successful")
            LaunchedEffect(uploadResponse.data) {
                if (uploadResponse.data) {
                    onSuccess()
                }
            }
        }

        else -> Unit
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}