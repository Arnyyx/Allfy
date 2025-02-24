package com.arny.allfy.presentation.common

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.arny.allfy.R
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screens
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch

@Composable
fun PostItem(
    initialPost: Post,
    currentUser: User,
    postViewModel: PostViewModel = hiltViewModel(),
    navController: NavController
) {
    val currentPost by postViewModel.currentPost.collectAsState()
    var post by remember { mutableStateOf(initialPost) }

    val likeLoadingStates by postViewModel.likeLoadingStates.collectAsState()
    LaunchedEffect(currentPost) {
        if (currentPost?.postID == initialPost.postID) {
            post = currentPost ?: initialPost
        }
    }

    val isLikeLoading by remember(likeLoadingStates) {
        derivedStateOf { likeLoadingStates[post.postID] ?: false }
    }
    val isLiked by remember(post) {
        derivedStateOf { post.likes.contains(currentUser.userID) }
    }
    val likeCount by remember(post) {
        derivedStateOf { post.likes.size }
    }
    val showComments = remember { mutableStateOf(false) }

    val deletePostState by postViewModel.deletePostState.collectAsState()
    LaunchedEffect(deletePostState) {
        if (deletePostState is Response.Success && (deletePostState as Response.Success).data) {
            navController.navigate(Screens.FeedScreen.route) {
                popUpTo(Screens.FeedScreen.route) {
                    inclusive = true
                }
            }
            Log.d("AAA", "Post deleted successfully")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!isLiked)
                            postViewModel.toggleLikePost(post, currentUser.userID)
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PostHeader(
                post, navController, currentUser,
                onEditPost = {
                    //Todo edit post
                },
                onDeletePost = {
                    postViewModel.deletePost(post.postID, post.postOwnerID)
                }
            )
            PostImages(post)
            if (post.caption.isNotBlank()) PostCaption(post.caption)
            PostActions(
                isLiked = isLiked,
                isLikeLoading = isLikeLoading,
                showComments = showComments,
                onLikeClick = {
                    postViewModel.toggleLikePost(post, currentUser.userID)
                }
            )
            if (likeCount > 0) LikeCount(likeCount)
        }
    }

    if (showComments.value) {
        CommentBottomSheet(
            post = post,
            currentUser = currentUser,
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
    onEditPost: () -> Unit = {},
    onDeletePost: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(8.dp)

    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = {
                navController.navigate("profile/${post.postOwnerID}")
            }
            )) {
            AsyncImage(
                model = post.postOwnerImageUrl,
                contentDescription = "User Avatar",
                placeholder = painterResource(R.drawable.ic_user),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = post.postOwnerUsername, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options"
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (post.postOwnerID == currentUser.userID) {
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
                            onDeletePost()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Post"
                            )
                        }
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
}

@Composable
private fun PostImages(post: Post) {
    if (post.imageUrls.isNotEmpty()) {
        val pagerState = rememberPagerState(
            initialPage = 0,
            initialPageOffsetFraction = 0f,
            pageCount = { post.imageUrls.size }
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
                AsyncImage(
                    model = post.imageUrls[page],
                    contentDescription = "Post Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(R.drawable.placehoder_image)
                )
            }

            if (post.imageUrls.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1}/${post.imageUrls.size}",
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

@Composable
private fun PostActions(
    isLiked: Boolean,
    isLikeLoading: Boolean,
    showComments: MutableState<Boolean>,
    onLikeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
            LikeButton(
                isLiked = isLiked,
                isLoading = isLikeLoading,
                onClick = onLikeClick
            )
            IconButton(onClick = {
                showComments.value = true
            }) {
                Icon(
                    painter = painterResource(R.drawable.ic_comment),
                    contentDescription = "Comment"
                )
            }
        }
        IconButton(onClick = { /* TODO: Implement Share */ }) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = "Share"
            )
        }
    }
}


@OptIn(DelicateCoroutinesApi::class)
@Composable
private fun LikeButton(
    isLiked: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale animation"
    )

    // Animation for color transition
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
        modifier = Modifier
            .scale(animatedScale)
    ) {
        IconButton(
            onClick = {
                onClick()
                scale = 0.8f
                kotlinx.coroutines.GlobalScope.launch {
                    kotlinx.coroutines.delay(50)
                    scale = 1f
                }
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


@Composable
private fun LikeCount(count: Int) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$count", fontWeight = FontWeight.Bold)
        Text(text = " likes", fontWeight = FontWeight.Bold)
    }
}