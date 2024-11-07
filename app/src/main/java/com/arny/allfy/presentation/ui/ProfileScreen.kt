package com.arny.allfy.presentation.ui

import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    postViewModel: PostViewModel,
    userId: String? = null
) {
    LaunchedEffect(userId) {
        if (!userId.isNullOrBlank()) {
            userViewModel.getUserById(userId)
        } else {
            userViewModel.getCurrentUser()
        }
    }

    val userState by if (userId != null) {
        userViewModel.otherUser.collectAsState()
    } else {
        userViewModel.currentUser.collectAsState()
    }

    when (userState) {
        is Response.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is Response.Error -> {
            Toast.makeText(
                LocalContext.current,
                "Error: ${(userState as Response.Error).message}",
                Toast.LENGTH_SHORT
            ).show()
        }

        is Response.Success -> {
            Scaffold(
                bottomBar = {
                    if (userId == null) {
                        BottomNavigation(
                            BottomNavigationItem.Profile,
                            navController
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                ) {
                    ProfileContent(
                        navController = navController,
                        user = (userState as Response.Success<User>).data,
                        postViewModel = postViewModel,
                        userViewModel = userViewModel,
                        isCurrentUser = userId == null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    navController: NavController,
    user: User,
    postViewModel: PostViewModel,
    userViewModel: UserViewModel,
    isCurrentUser: Boolean
) {
    var isFollowing by remember { mutableStateOf(false) }
    val currentUserState by userViewModel.currentUser.collectAsState()

    LaunchedEffect(currentUserState) {
        if (currentUserState is Response.Success && !isCurrentUser) {
            val currentUser = (currentUserState as Response.Success<User>).data
            isFollowing = currentUser.following.contains(user.userID)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user.userName) },
                actions = {
                    if (isCurrentUser) {
                        IconButton(onClick = {
                            navController.navigate(Screens.CreatePostScreen.route)
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "New Post")
                        }
                        IconButton(onClick = {
                            navController.navigate(Screens.SettingsScreen.route)
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Settings")
                        }
                    }
                }, navigationIcon = {
                    if (isCurrentUser) return@TopAppBar
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.imageUrl)
                        .placeholder(R.drawable.ic_user)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = user.userName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.bio,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatisticItem("Posts", user.postsIDs.size.toString())
                StatisticItem("Followers", user.followers.size.toString())
                StatisticItem("Following", user.following.size.toString())
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isCurrentUser) {
                OutlinedButton(
                    onClick = {
                        navController.navigate(Screens.EditProfileScreen.route)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Profile")
                }
            } else {
                Button(
                    onClick = {
                        if (isFollowing) {
                            userViewModel.unfollowUser(user.userID)
                        } else {
                            userViewModel.followUser(user.userID)
                        }
                        isFollowing = !isFollowing
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing)
                            MaterialTheme.colorScheme.surface
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        if (isFollowing) "Following" else "Follow",
                        color = if (isFollowing)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PostsGrid(navController, user.postsIDs, postViewModel)
        }
    }
}

@Composable
fun StatisticItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 14.sp
        )
    }
}

@Composable
fun PostsGrid(
    navController: NavController,
    userPostIds: List<String>,
    postViewModel: PostViewModel,
    modifier: Modifier = Modifier
) {
    var loadedPosts by remember { mutableStateOf<Map<String, Post>>(emptyMap()) }

    LaunchedEffect(userPostIds) {
        userPostIds.forEach { postId ->
            postViewModel.getPostByID(postId)
        }
    }

    val postsState = postViewModel.postsState.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        items(
            items = userPostIds,
            key = { it }
        ) { postId ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clickable {
                        val post = loadedPosts[postId]
                        if (post != null) {
                            navController.navigate("postDetail/${post.postID}")
                        }
                    }
            ) {
                when (val post = loadedPosts[postId]) {
                    null -> {
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

        else -> {}
    }


}