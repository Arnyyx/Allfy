package com.arny.allfy.presentation.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.BottomNavigationItem
import com.arny.allfy.presentation.common.BottomNavigation
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screens

@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel
) {
    userViewModel.getCurrentUser()
    when (val response = userViewModel.getCurrentUser.value) {
        is Response.Loading -> {
            CircularProgressIndicator()
        }

        is Response.Success -> {
            val user = response.data
            Scaffold(bottomBar = {
                BottomNavigation(
                    BottomNavigationItem.Profile,
                    navController
                )
            }) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                ) {
                    ProfileScreen(navController, user, postViewModel)
                }
            }
        }

        is Response.Error -> {}
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, user: User, postViewModel: PostViewModel) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Profile") }, actions = {
            IconButton(onClick = {
                navController.navigate(Screens.CreatePostScreen.route)
            }) {
                Icon(Icons.Default.Add, contentDescription = "New Post")
            }
            IconButton(onClick = {
                // Navigate to settings screen
                navController.navigate(Screens.SettingsScreen.route)
            }) {
                Icon(Icons.Default.Menu, contentDescription = "Settings")
            }
        })
    }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Profile Header
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.imageUrl)
                        .placeholder(R.drawable.ic_user)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = user.userName, fontSize = 20.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.bio, fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatisticItem("Posts", user.postsIDs.size.toString())
                StatisticItem("Followers", user.followers.size.toString())
                StatisticItem("Following", user.following.size.toString())
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit Profile Button
            OutlinedButton(
                onClick = {
                    navController.navigate(Screens.EditProfileScreen.route)
                }, modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Profile")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Posts
            PostsGrid(navController, user.postsIDs, postViewModel)
        }
    }
}

@Composable
fun PostsGrid(
    navController: NavController,
    userPostIds: List<String>,
    postViewModel: PostViewModel,
    modifier: Modifier = Modifier
) {
    // State để track posts đã load
    var loadedPosts by remember { mutableStateOf<Map<String, Post>>(emptyMap()) }

    // Effect để load tất cả posts một lần
    LaunchedEffect(userPostIds) {
        userPostIds.forEach { postId ->
            postViewModel.getPost(postId)
        }
    }

    // Observe state changes
    val postsState = postViewModel.postsState.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        items(
            items = userPostIds,
            key = { it } // Use postId as key for better performance
        ) { postId ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clickable {
                        val post = loadedPosts[postId]
                        if (post != null) {
                            navController.navigate("postDetail/${post.id}")
                        }
                    }
            ) {
                when (val post = loadedPosts[postId]) {
                    null -> {
                        // Loading placeholder
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    else -> {
                        // Post image
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(post.imageUrls.firstOrNull())
                                .placeholder(R.drawable.placehoder_image)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Post Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }

    // Handle state changes
    val context = LocalContext.current
    when (val state = postsState.value) {
        is Response.Success -> {
            loadedPosts = state.data
        }

        is Response.Error -> {
            LaunchedEffect(state) {
                Toast.makeText(
                    context, "Error: ${state.message}", Toast.LENGTH_SHORT
                ).show()
                Log.e("PostsGrid", "Error loading posts: ${state.message}")
            }
        }

        else -> {} // Loading state is handled by individual items
    }


}

@Composable
fun StatisticItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold
        )
        Text(
            text = label, fontSize = 14.sp
        )
    }
}