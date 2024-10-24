package com.arny.allfy.presentation.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.arny.allfy.utils.Response

@Composable
fun PostItem(
    post: Post,
    currentUser: User,
    onPostClick: () -> Unit,
) {
    val postViewModel: PostViewModel = hiltViewModel()

    // Theo dõi trạng thái liked cục bộ
    val (isLocalLiked, setLocalLiked) = remember {
        mutableStateOf(post.likes.contains(currentUser.userID))
    }

    // Theo dõi số lượng like cục bộ
    val (localLikeCount, setLocalLikeCount) = remember {
        mutableIntStateOf(post.likes.size)
    }

    // Animation cho icon
    val scale by animateFloatAsState(
        targetValue = if (isLocalLiked) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // Animation cho số lượng like
    val animatedLikeCount by animateIntAsState(
        targetValue = localLikeCount,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val likeState by postViewModel.postsLikeState

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onPostClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
    ) {


        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(8.dp)
            ) {
                AsyncImage(
                    model = post.postOwnerImageUrl,
                    contentDescription = "User Avatar",
                    placeholder = painterResource(R.drawable.ic_user),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                )

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = post.postOwnerUsername,
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
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                setLocalLiked(!isLocalLiked)
                                setLocalLikeCount(if (!isLocalLiked) localLikeCount + 1 else localLikeCount - 1)
                                post.likes = if (isLocalLiked) post.likes + currentUser.userID else post.likes - currentUser.userID
                                postViewModel.toggleLikePost(post, currentUser.userID)
                            },
                            enabled = likeState !is Response.Loading
                        ) {
                            when (likeState) {
                                is Response.Loading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }

                                is Response.Error -> {
                                    LaunchedEffect(Unit) {
                                        setLocalLiked(post.likes.contains(currentUser.userID))
                                        setLocalLikeCount(post.likes.size)
                                    }
                                    Icon(
                                        imageVector = if (isLocalLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Like",
                                        modifier = Modifier.scale(scale),
                                        tint = if (isLocalLiked) Color.Red else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                is Response.Success -> {
                                    Icon(
                                        imageVector = if (isLocalLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Like",
                                        modifier = Modifier.scale(scale),
                                        tint = if (isLocalLiked) Color.Red else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
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

            // Like count với animation
            if (localLikeCount > 0) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$animatedLikeCount",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.graphicsLayer {
                            scaleX = if (isLocalLiked) 1.2f else 1f
                            scaleY = if (isLocalLiked) 1.2f else 1f
                        }
                    )
                    Text(
                        text = " likes",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
