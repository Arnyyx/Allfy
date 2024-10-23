package com.arny.allfy.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.arny.allfy.R
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale

@Composable
fun PostItem(post: Post, onPostClick: () -> Unit) {
    val userViewModel: UserViewModel = hiltViewModel()
    val postViewModel: PostViewModel = hiltViewModel()

    // Fetch post owner info
    userViewModel.getUser(post.userID)
    userViewModel.getCurrentUser()

    val currentUserResponse = userViewModel.getCurrentUser.value
    val postUserResponse = userViewModel.getUserData.value

    var isLiked by remember { mutableStateOf(false) }

    when (currentUserResponse) {
        is Response.Loading -> {
        }

        is Response.Success -> {
            val currentUser = currentUserResponse.data
            isLiked = post.likes.contains(currentUser.userID)
        }

        is Response.Error -> {} // Keep previous state
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onPostClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
    ) {
        when (postUserResponse) {
            is Response.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is Response.Success -> {
                val postUser = postUserResponse.data
                Column(modifier = Modifier.fillMaxWidth()) {
                    // User Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(8.dp)
                    ) {
                        AsyncImage(
                            model = postUser.imageUrl,
                            contentDescription = "User Avatar",
                            placeholder = painterResource(R.drawable.ic_user),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = postUser.userName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Image carousel
                    if (post.imageUrls.isNotEmpty()) {
                        val pagerState = rememberPagerState(
                            initialPage = 0,
                            initialPageOffsetFraction = 0f,
                            pageCount = { post.imageUrls.size }
                        )
                        Box {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                            ) { page ->
                                Image(
                                    painter = rememberAsyncImagePainter(post.imageUrls[page]),
                                    contentDescription = "Post Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
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

                    // Caption
                    if (post.caption.isNotBlank()) {
                        Text(
                            text = post.caption,
                            modifier = Modifier
                                .padding(8.dp)
                        )
                    }

                    // Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            // Like animation
                            val scale by animateFloatAsState(targetValue = if (isLiked) 1.2f else 1f)

                            IconButton(
                                onClick = {
                                    // Only toggle like if we have current user
                                    when (currentUserResponse) {
                                        is Response.Success -> {
                                            val currentUser = currentUserResponse.data
                                            postViewModel.toggleLikePost(
                                                post.id,
                                                currentUser.userID
                                            )
                                            isLiked = !isLiked
                                        }

                                        else -> {
                                            // Optionally show login prompt or handle error
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Like",
                                    modifier = Modifier.scale(scale),
                                )

                            }

                            IconButton(onClick = { /* TODO: Implement Comment */ }) {
                                Icon(
                                    imageVector = Icons.Default.MailOutline,
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

                    // Like count (optional)
                    if (post.likes.isNotEmpty()) {
                        Text(
                            text = "${post.likes.size} likes",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            is Response.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error loading user: ${postUserResponse.message}",
                        color = Color.Red
                    )
                }
            }
        }
    }
}
