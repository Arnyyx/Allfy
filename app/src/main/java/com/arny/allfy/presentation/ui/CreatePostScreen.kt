package com.arny.allfy.presentation.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
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
import com.arny.allfy.presentation.viewmodel.PostState
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
    val context = LocalContext.current
    val userState by userViewModel.userState.collectAsState()
    val postState by postViewModel.postState.collectAsState()
    var captionText by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val validUris = uris.filter { uri ->
            val mimeType = context.contentResolver.getType(uri)
            mimeType?.startsWith("image/") == true || mimeType?.startsWith("video/") == true
        }
        selectedImageUris = validUris
    }
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { selectedImageUris.size }
    )

    Scaffold(
        topBar = {
            Column {
                CreatePostTopBar(
                    onCancelClick = { navHostController.popBackStack() },
                    onShareClick = {
                        if (captionText.isBlank() && selectedImageUris.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Please add content to share",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@CreatePostTopBar
                        }
                        val post = Post(
                            postOwnerID = userState.currentUser.userId,
                            postOwnerUsername = userState.currentUser.username,
                            postOwnerImageUrl = userState.currentUser.imageUrl,
                            caption = captionText,
                            mediaItems = emptyList()
                        )
                        postViewModel.uploadPost(post, selectedImageUris)
                    },
                    isLoading = postState.isUploadingPost,
                    isShareEnabled = captionText.isNotBlank() || selectedImageUris.isNotEmpty()
                )
                if (postState.isUploadingPost) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImageWithPlaceholder(
                        imageUrl = userState.currentUser.imageUrl,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = userState.currentUser.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }

            if (selectedImageUris.isNotEmpty()) {
                item {
                    ImagePickerSection(
                        selectedImageUris = selectedImageUris,
                        pagerState = pagerState,
                        onImagePick = { launcher.launch("*/*") },
                        onRemoveImage = { index ->
                            selectedImageUris =
                                selectedImageUris.toMutableList().apply { removeAt(index) }
                        }
                    )
                }
            }

            item {
                CaptionInput(
                    captionText = captionText,
                    onCaptionChange = { captionText = it }
                )
            }

            item {
                PostOptions(
                    onAddMedia = { launcher.launch("*/*") }
                )
            }

            item {
                UploadStateHandler(
                    postState = postState,
                    postViewModel = postViewModel,
                    onSuccess = { navHostController.popBackStack() }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostTopBar(
    isLoading: Boolean,
    isShareEnabled: Boolean,
    onCancelClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = "Create Post",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onCancelClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            TextButton(
                onClick = onShareClick,
                enabled = isShareEnabled && !isLoading,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "Post",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isShareEnabled && !isLoading)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.shadow(1.dp)
    )
}

@Composable
private fun ImagePickerSection(
    selectedImageUris: List<Uri>,
    pagerState: PagerState,
    onImagePick: () -> Unit,
    onRemoveImage: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val uri = selectedImageUris[page]
            val mimeType = LocalContext.current.contentResolver.getType(uri)

            Box(modifier = Modifier.fillMaxSize()) {
                if (mimeType?.startsWith("image/") == true) {
                    AsyncImageWithPlaceholder(
                        imageUrl = uri.toString(),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else if (mimeType?.startsWith("video/") == true) {
                    VideoPlayer(uri = uri, modifier = Modifier.fillMaxSize())
                }

                IconButton(
                    onClick = { onRemoveImage(page) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.White
                    )
                }
            }
        }

        if (selectedImageUris.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1}/${selectedImageUris.size}",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
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
        onValueChange = { if (it.length <= 2200) onCaptionChange(it) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = {
            Text(
                "Write a caption...",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        maxLines = 20,
        supportingText = {
            Text(
                text = "${captionText.length}/2200",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    )
}

@Composable
private fun PostOptions(
    onAddMedia: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onAddMedia) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Media",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Row {
            IconButton(onClick = { /* Tag people */ }) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Tag People",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { /* Add location */ }) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Add Location",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UploadStateHandler(
    postState: PostState,
    postViewModel: PostViewModel,
    onSuccess: () -> Unit
) {
    when {
        postState.isUploadingPost -> {}

        postState.uploadPostError.isNotEmpty() -> {
            Text(
                text = postState.uploadPostError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        }

        postState.uploadPostSuccess -> {
            Toast.makeText(LocalContext.current, "Upload successful", Toast.LENGTH_SHORT).show()
            LaunchedEffect(Unit) {
                onSuccess()
                postViewModel.clearUploadPostState()
            }
        }
    }
}