package com.arny.allfy.presentation.common

import android.icu.text.DecimalFormat
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.arny.allfy.R
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.components.StoryRingAvatar
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screen
import com.arny.allfy.utils.getDataOrNull
import com.arny.allfy.utils.toTimeAgo

@Composable
fun PostItem(
    post: Post,
    currentUser: User,
    postViewModel: PostViewModel = hiltViewModel(),
    navController: NavController
) {
    val postState by postViewModel.postState.collectAsState()
    val loadingPosts by postViewModel.loadingPosts.collectAsState()
    val context = LocalContext.current

    val isLiked by remember(postState, post, currentUser) {
        derivedStateOf {
            val singlePost = postState.getPostState.getDataOrNull()
            val feedPost =
                postState.feedPostsState.getDataOrNull()?.find { it.postID == post.postID }
            (singlePost?.postID == post.postID && singlePost.likes.contains(currentUser.userId))
                    || (feedPost?.likes?.contains(currentUser.userId) ?: post.likes.contains(
                currentUser.userId
            ))
        }
    }
    val likeCount by remember(postState, post) {
        derivedStateOf {
            val singlePost = postState.getPostState.getDataOrNull()
            val feedPost =
                postState.feedPostsState.getDataOrNull()?.find { it.postID == post.postID }
            if (singlePost?.postID == post.postID) singlePost.likes.size
            else feedPost?.likes?.size ?: post.likes.size
        }
    }
    val commentCount by remember(postState, post) {
        derivedStateOf {
            val singlePost = postState.getPostState.getDataOrNull()
            val feedPost =
                postState.feedPostsState.getDataOrNull()?.find { it.postID == post.postID }
            if (singlePost?.postID == post.postID) singlePost.commentCount
            else feedPost?.commentCount ?: post.commentCount
        }
    }
    val isLikeLoading by remember(loadingPosts, post) {
        derivedStateOf { loadingPosts[post.postID]?.contains("like") ?: false }
    }
    val isCommentLoading by remember(loadingPosts, post) {
        derivedStateOf { loadingPosts[post.postID]?.contains("comment") ?: false }
    }
    val isDeleteLoading by remember(loadingPosts, post) {
        derivedStateOf { loadingPosts[post.postID]?.contains("delete") ?: false }
    }
    val showComments = remember { mutableStateOf(false) }

    LaunchedEffect(postState.deletePostState) {
        when (val deleteState = postState.deletePostState) {
            is Response.Success -> {
                navController.navigate(Screen.ProfileScreen()) {
                    popUpTo(Screen.SplashScreen) { inclusive = true }
                }
                postViewModel.resetDeletePostState()
            }

            is Response.Error -> {
                Toast.makeText(
                    context,
                    "Failed to delete post: ${deleteState.message}",
                    Toast.LENGTH_LONG
                ).show()
                postViewModel.resetDeletePostState()
            }

            else -> {}
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isLiked) {
                            postViewModel.toggleLikePost(post, currentUser.userId)
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PostHeader(
                post = post,
                navController = navController,
                currentUser = currentUser,
                isDeletingPost = isDeleteLoading,
                onEditPost = {
                    navController.navigate(Screen.PostEditorScreen(post.postID))
                },
                onDeletePost = { postViewModel.deletePost(post.postID, post.postOwnerID) }
            )
            if (post.mediaItems.isNotEmpty()) PostImages(post)
            if (post.caption.isNotBlank()) PostCaption(post.caption)
            PostActions(
                isLiked = isLiked,
                isLikeLoading = isLikeLoading,
                likeCount = likeCount,
                commentCount = commentCount,
                showComments = showComments,
                onLikeClick = { postViewModel.toggleLikePost(post, currentUser.userId) }
            )
            if (isLikeLoading || isCommentLoading || isDeleteLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }

    if (showComments.value) {
        CommentBottomSheet(
            postId = post.postID,
            currentUser = currentUser,
            postViewModel = postViewModel,
            isVisible = showComments.value,
            onDismiss = { showComments.value = false }
        )
    }
}

@Composable
private fun PostHeader(
    post: Post,
    navController: NavController,
    currentUser: User,
    isDeletingPost: Boolean = false,
    onEditPost: () -> Unit = {},
    onDeletePost: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val decimalFormat = remember { DecimalFormat("0.000") }
    val formattedScore = post.score.let { decimalFormat.format(it) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    navController.navigate(Screen.ProfileScreen(post.postOwnerID))
                }
            ) {
                StoryRingAvatar(
                    imageUrl = post.postOwner.imageUrl.ifEmpty { null },
                    hasStory = post.postOwner.hasStory,
                    size = 40.dp,
                    strokeWidth = 1.dp,
                    onClick = {
                        navController.navigate(Screen.ProfileScreen(post.postOwnerID))
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = post.postOwner.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = post.timestamp.toTimeAgo(),
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row {
                        Text(
                            text = formattedScore,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (post.score > 0.7f)
                            Text(
                                text = post.reason,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                    }

                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { showMenu = true }) {
                    if (isDeletingPost) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options"
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMenu && !isDeletingPost,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (post.postOwnerID == currentUser.userId) {
                        DropdownMenuItem(
                            text = { Text("Edit Post") },
                            onClick = {
                                onEditPost()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Post"
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Post") },
                            onClick = {
                                showDeleteConfirmDialog = true
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Post"
                                )
                            },
                            enabled = !isDeletingPost
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Report Post") },
                        onClick = {
                            showMenu = false
                        }
                    )
                }
            }
        }

        if (isDeletingPost) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    if (showDeleteConfirmDialog) {
        Dialog(
            title = "Delete Post?",
            message = "Are you sure you want to delete this post?",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = {
                onDeletePost()
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}

@Composable
private fun PostImages(post: Post) {
    if (post.mediaItems.isNotEmpty()) {
        val pagerState = rememberPagerState(
            initialPage = 0,
            initialPageOffsetFraction = 0f,
            pageCount = { post.mediaItems.size }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(
                        ratio = 1f,
                        matchHeightConstraintsFirst = true
                    )
            ) { page ->
                val mediaItem = post.mediaItems[page]
                when (mediaItem.mediaType) {
                    "video" -> {
                        VideoPlayer(
                            url = mediaItem.url,
                            thumbnailUrl = mediaItem.thumbnailUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    "image" -> {
                        AsyncImage(
                            model = mediaItem.url,
                            contentDescription = "Post Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            placeholder = painterResource(R.drawable.placehoder_image)
                        )
                    }

                    "audio" -> {
                        Text(
                            text = "Audio not supported yet",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            if (post.mediaItems.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1}/${post.mediaItems.size}",
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
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
}

@Composable
private fun PostCaption(caption: String) {
    Text(text = caption, modifier = Modifier.padding(8.dp))
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    url: String,
    thumbnailUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    var isVideoReady by remember { mutableStateOf(false) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    isVideoReady = true
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    isVideoReady = true
                }
            }
        }
        exoPlayer.addListener(listener)
        exoPlayer.playWhenReady = true

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(modifier = modifier) {
        if (!isVideoReady && thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Video Thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(R.drawable.placehoder_image)
            )
        }
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    controllerAutoShow = false
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isVideoReady) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun PostActions(
    isLiked: Boolean,
    isLikeLoading: Boolean,
    likeCount: Int,
    commentCount: Int,
    showComments: androidx.compose.runtime.MutableState<Boolean>,
    onLikeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LikeButton(
                    isLiked = isLiked,
                    isLoading = isLikeLoading,
                    onClick = onLikeClick
                )
                if (likeCount > 0) {
                    Text(
                        text = likeCount.toString(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { showComments.value = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_comment),
                        contentDescription = "Comment"
                    )
                }
                if (commentCount > 0) {
                    Text(
                        text = commentCount.toString(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun LikeButton(
    isLiked: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale animation"
    )

    val animatedColor by animateColorAsState(
        targetValue = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "color animation"
    )
    val animatedSize by animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "size animation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.scale(animatedScale)
    ) {
        IconButton(
            onClick = {
                onClick()
                scale = 0.8f
            },
            enabled = !isLoading,
            modifier = Modifier.scale(animatedSize)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Like",
                    tint = animatedColor,
                    modifier = Modifier.graphicsLayer(
                        scaleX = animatedSize,
                        scaleY = animatedSize
                    )
                )
            }
        }
    }
}