package com.arny.allfy.presentation.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.BottomNavigation
import com.arny.allfy.presentation.common.BottomNavigationItem
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screens

@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel = hiltViewModel(),
    postViewModel: PostViewModel = hiltViewModel(),
    userId: String? = null
) {
    LaunchedEffect(Unit) {
        if (userId == null) {
            userViewModel.getCurrentUser()
        }
        if (userId != null) {
            userViewModel.getUserById(userId)
        }
    }

    val userState by if (userId != null) {
        userViewModel.otherUser.collectAsState()
    } else {
        userViewModel.currentUser.collectAsState()
    }

    LaunchedEffect(userId, userState) {
        val effectiveUserId = if (userId != null) {
            userId
        } else if (userState is Response.Success) {
            (userState as Response.Success<User>).data.userId
        } else {
            null
        }

        if (effectiveUserId != null) {
            userViewModel.getFollowingCount(effectiveUserId)
            userViewModel.getFollowersCount(effectiveUserId)
            userViewModel.getPostsIdsFromSubcollection(effectiveUserId)
        }
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
    val followingCountState by userViewModel.followingCount.collectAsState()
    val followersCountState by userViewModel.followersCount.collectAsState()
    val postsIdsState by userViewModel.postsIds.collectAsState()

    LaunchedEffect(currentUserState) {
        if (currentUserState is Response.Success && !isCurrentUser) {
            val currentUser = (currentUserState as Response.Success<User>).data
            userViewModel.checkIfFollowing(currentUser.userId, user.userId).collect { response ->
                if (response is Response.Success) {
                    isFollowing = response.data
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user.username) },
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
                },
                navigationIcon = {
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
                Image(
                    painter = rememberAsyncImagePainter(
                        model = user.imageUrl,
                        placeholder = rememberAsyncImagePainter(R.drawable.ic_user),
                        error = rememberAsyncImagePainter(R.drawable.ic_user)
                    ),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = user.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.username,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
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
                StatisticItem(
                    "Posts",
                    when (val postsIds = postsIdsState) {
                        is Response.Success -> postsIds.data.size.toString()
                        else -> "0"
                    }
                )
                StatisticItem(
                    "Followers",
                    when (val followersCount = followersCountState) {
                        is Response.Success -> followersCount.data.toString()
                        else -> "0"
                    }
                )
                StatisticItem(
                    "Following",
                    when (val followingCount = followingCountState) {
                        is Response.Success -> followingCount.data.toString()
                        else -> "0"
                    }
                )
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isFollowing) {
                                userViewModel.unfollowUser(user.userId)
                            } else {
                                userViewModel.followUser(user.userId)
                            }
                            isFollowing = !isFollowing
                        },
                        modifier = Modifier.weight(1f),
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
                    OutlinedButton(
                        onClick = {
                            if (currentUserState is Response.Success) {
                                val currentUser = (currentUserState as Response.Success<User>).data
                                navController.navigate("chat/${currentUser.userId}/${user.userId}")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Message")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PostsGrid(navController, postsIdsState, postViewModel)
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
    postsIdsState: Response<List<String>>,
    postViewModel: PostViewModel,
    modifier: Modifier = Modifier
) {
    var loadedPosts by remember { mutableStateOf<Map<String, Post>>(emptyMap()) }

    LaunchedEffect(postsIdsState) {
        if (postsIdsState is Response.Success) {
            val postIds = postsIdsState.data
            postIds.forEach { postId ->
                postViewModel.getPostByID(postId)
            }
        }
    }

    val postsState by postViewModel.postsState.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        if (postsIdsState is Response.Success) {
            items(
                items = postsIdsState.data,
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
    }

    val context = LocalContext.current
    when (val state = postsState) {
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