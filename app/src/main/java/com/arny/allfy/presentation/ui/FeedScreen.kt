package com.arny.allfy.presentation.ui

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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.arny.allfy.R
import com.arny.allfy.domain.model.Post
import com.arny.allfy.presentation.common.BottomNavigationItem
import com.arny.allfy.presentation.common.BottomNavigation
import com.arny.allfy.presentation.common.Toast
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.utils.Response

@Composable
fun FeedScreen(navController: NavController) {
    val postViewModel: PostViewModel = hiltViewModel()
    val state by postViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        postViewModel.loadPosts(userID = "currentUserId") //TODO: Thay "currentUserId" bằng ID người dùng hiện tại
    }

    Scaffold(
        bottomBar = { BottomNavigation(BottomNavigationItem.Profile, navController) }
    ) { paddingValues ->
        if (state.isLoading && state.posts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.error.isNotBlank()) {
            // Hiển thị thông báo lỗi
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(text = state.error, color = Color.Red)
            }
        } else {
            // Hiển thị danh sách bài đăng
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(state.posts) { post ->
                    PostItem(post = post, onPostClick = { })
                }

                // Tải thêm bài đăng khi cuộn đến cuối danh sách
                if (!state.endReached && !state.isLoading) {
                    item {
                        LaunchedEffect(Unit) {
                            postViewModel.loadPosts(userID = "currentUserId")
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }


}

@Composable
fun PostItem(post: Post, onPostClick: () -> Unit) {
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
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(R.drawable.ic_logo),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "post.username",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Carousel ảnh
            if (post.imageUrls.isNotEmpty()) {
                val pagerState = rememberPagerState(
                    initialPage = 0,
                    initialPageOffsetFraction = 0f,
                    pageCount = { post.imageUrls.size }
                )
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

                // Chỉ số trang
                if (post.imageUrls.size > 1) {
                    Text(
                        text = "${pagerState.currentPage + 1}/${post.imageUrls.size}",
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
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

            // Actions: Like, Comment, Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    IconButton(onClick = { /* TODO: Implement Like */ }) {
                        Icon(
                            imageVector = Icons.Filled.FavoriteBorder,
                            contentDescription = "Like"
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
        }
    }
}

@Preview()
@Composable
fun PostItemPreview() {
    val samplePost = Post(
        id = "1",
        userID = "user1",
        imageUrls = listOf(
            "https://example.com/image1.jpg",
            "https://example.com/image2.jpg"
        ),
        caption = "This is a sample post caption.",
        timestamp = System.currentTimeMillis(),
        likes = 120,
        comments = 30
    )
    PostItem(post = samplePost, onPostClick = {})
}

