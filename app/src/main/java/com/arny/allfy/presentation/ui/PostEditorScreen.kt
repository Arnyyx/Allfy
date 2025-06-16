package com.arny.allfy.presentation.ui

import android.net.Uri
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.arny.allfy.presentation.state.PostState
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Screen
import com.arny.allfy.utils.getDataOrNull
import com.arny.allfy.utils.getErrorMessageOrNull
import com.arny.allfy.utils.isError
import com.arny.allfy.utils.isLoading
import com.arny.allfy.utils.isSuccess
import androidx.core.net.toUri

@Composable
fun PostEditorScreen(
    navHostController: NavHostController,
    postViewModel: PostViewModel,
    userViewModel: UserViewModel,
    postId: String? = null
) {
    val context = LocalContext.current
    val userState by userViewModel.userState.collectAsState()
    val postState by postViewModel.postState.collectAsState()
    var captionText by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var mediaItemsToRemove by remember { mutableStateOf<List<String>>(emptyList()) }

    val currentUser = userState.currentUserState.getDataOrNull() ?: User()
    val isEditing = postId != null

    LaunchedEffect(postId) {
        if (isEditing && postId != null) {
            postViewModel.getPostByID(postId)
        }
    }

    LaunchedEffect(postState.getPostState) {
        if (isEditing && postState.getPostState.isSuccess) {
            postState.getPostState.getDataOrNull()?.let { post ->
                captionText = post.caption
                selectedImageUris = post.mediaItems
                    .filter { it.mediaType == "image" || it.mediaType == "video" }
                    .map { it.url.toUri() }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val validUris = uris.filter { uri ->
            val mimeType = context.contentResolver.getType(uri)
            mimeType?.startsWith("image/") == true || mimeType?.startsWith("video/") == true
        }
        selectedImageUris = selectedImageUris + validUris
    }
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { selectedImageUris.size }
    )

    Scaffold(
        topBar = {
            Column {
                TopBar(
                    isEditing = isEditing,
                    isLoading = postState.uploadPostState.isLoading || postState.editPostState.isLoading,
                    isShareEnabled = captionText.isNotBlank() || selectedImageUris.isNotEmpty(),
                    onCancelClick = { navHostController.popBackStack() },
                    onShareClick = {
                        if (captionText.isBlank() && selectedImageUris.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Please add content to ${if (isEditing) "edit" else "share"}",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@TopBar
                        }
                        if (isEditing && postId != null) {
                            val newImageUris =
                                selectedImageUris.filter { !it.toString().startsWith("https://") }
                            postViewModel.editPost(
                                postID = postId,
                                userID = currentUser.userId,
                                newCaption = captionText,
                                newImageUris = newImageUris,
                                mediaItemsToRemove = mediaItemsToRemove
                            )
                        } else {
                            val post = Post(
                                postOwnerID = currentUser.userId,
                                caption = captionText,
                                mediaItems = emptyList()
                            )
                            postViewModel.uploadPost(post, selectedImageUris)
                        }
                    },
                )
                if (postState.uploadPostState.isLoading || postState.editPostState.isLoading) {
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
                        imageUrl = currentUser.imageUrl,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = currentUser.username,
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
                            if (isEditing) {
                                val removedUrl = selectedImageUris[index].toString()
                                if (removedUrl.startsWith("https://")) {
                                    mediaItemsToRemove = mediaItemsToRemove + removedUrl
                                }
                            }
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
                    onSuccess = {
                        navHostController.navigate(Screen.ProfileScreen()) {
                            popUpTo(Screen.PostEditorScreen()) { inclusive = true }
                        }
                    },
                    isEditing = isEditing
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    isEditing: Boolean,
    isLoading: Boolean,
    isShareEnabled: Boolean,
    onCancelClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = if (isEditing) "Editing Post" else "Create Post",
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
                    text = if (isEditing) "Save" else "Post",
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
            val mimeType = if (uri.scheme == "content" || uri.scheme == "file") {
                LocalContext.current.contentResolver.getType(uri)
            } else {
                // Assume Firebase URL is image or video based on extension or metadata
                if (uri.toString().endsWith(".mp4")) "video/mp4" else "image/jpeg"
            }

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
    onSuccess: () -> Unit,
    isEditing: Boolean
) {
    when {
        postState.uploadPostState.isLoading || postState.editPostState.isLoading -> {}

        postState.uploadPostState.isError -> {
            postState.uploadPostState.getErrorMessageOrNull()?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        postState.editPostState.isError -> {
            postState.editPostState.getErrorMessageOrNull()?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        postState.uploadPostState.isSuccess || postState.editPostState.isSuccess -> {
            Toast.makeText(
                LocalContext.current,
                if (isEditing) "Post edited successfully" else "Upload successful",
                Toast.LENGTH_SHORT
            ).show()
            LaunchedEffect(Unit) {
                onSuccess()
                if (isEditing) {
                    postViewModel.resetEditPostState()
                    postViewModel.resetGetPostState()
                } else {
                    postViewModel.resetUploadPostState()
                }
            }
        }
    }
}