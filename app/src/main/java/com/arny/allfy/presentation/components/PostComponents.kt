package com.arny.allfy.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.Post
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screen

@Composable
fun PostsGrid(
    navController: NavController,
    postsIds: List<String>,
    postViewModel: PostViewModel,
    showMediaPosts: Boolean
) {
    LaunchedEffect(postsIds) {
        if (postsIds.isNotEmpty()) {
            postViewModel.getPostsByIds(postsIds)
        }
    }
    val postState by postViewModel.postState.collectAsState()
    when (val postsResponse = postState.getPostsState) {
        is Response.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator()
            }
        }

        is Response.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = postsResponse.message, color = MaterialTheme.colorScheme.error)
            }
        }

        is Response.Success -> {
            val loadedPosts = postsResponse.data.associateBy { it.postID }
            val filteredPostIds = postsIds.filter { postId ->
                loadedPosts[postId]?.let { post ->
                    if (showMediaPosts) post.mediaItems.isNotEmpty() else post.mediaItems.isEmpty()
                } ?: false
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (showMediaPosts) 3 else 1),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(filteredPostIds, key = { _, postId -> postId }) { _, postId ->
                    if (showMediaPosts) {
                        AnimatedPostThumbnail(postId, loadedPosts, navController)
                    } else {
                        TextOnlyPostItem(postId, loadedPosts, navController)
                    }
                }
            }
        }

        is Response.Idle -> {}
    }
}

@Composable
fun TextOnlyPostItem(
    postId: String,
    loadedPosts: Map<String, Post>,
    navController: NavController
) {
    val post = loadedPosts[postId] ?: return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .clickable { navController.navigate(Screen.PostDetailScreen(postId)) }
            .padding(12.dp)
    ) {
        Text(
            text = post.caption,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AnimatedPostThumbnail(
    postId: String,
    loadedPosts: Map<String, Post>,
    navController: NavController
) {
    val post = loadedPosts[postId] ?: return
    val context = LocalContext.current
    val firstMediaItem = post.mediaItems.firstOrNull()
    val imageUrl = when (firstMediaItem?.mediaType) {
        "image" -> firstMediaItem.url
        "video" -> firstMediaItem.thumbnailUrl
        else -> null
    }
    AnimatedVisibility(
        visible = imageUrl != null,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = fadeOut()
    ) {
        imageUrl?.let {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(it)
                    .placeholder(R.drawable.placehoder_image)
                    .build(),
                contentDescription = "Post Thumbnail",
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { navController.navigate(Screen.PostDetailScreen(postId)) },
                contentScale = ContentScale.Crop
            )
        }
    }
}