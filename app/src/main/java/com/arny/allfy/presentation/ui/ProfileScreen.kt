package com.arny.allfy.presentation.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.arny.allfy.presentation.common.BottomNavigation
import com.arny.allfy.presentation.common.BottomNavigationItem
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel,
    userId: String? = null
) {
    val userState by if (userId != null) userViewModel.otherUser.collectAsState() else userViewModel.currentUser.collectAsState()
    val isLoading by remember { derivedStateOf { userState is Response.Loading } }

    LaunchedEffect(key1 = userId) {
        if (userId == null) userViewModel.getCurrentUser() else userViewModel.getUserById(userId)
    }

    LaunchedEffect(key1 = userState) {
        if (userState is Response.Success) {
            val user = (userState as Response.Success<User>).data
            with(userViewModel) {
                getFollowingCount(user.userId)
                getFollowersCount(user.userId)
                getPostsIdsFromSubcollection(user.userId)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            val currentUserState = userState // Biến tạm
                            Text(
                                text = if (currentUserState is Response.Success) currentUserState.data.username else "",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            if (userId != null) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                }
                            }
                        },
                        actions = {
                            if (userId == null) {
                                IconButton(onClick = { navController.navigate(Screens.CreatePostScreen.route) }) {
                                    Icon(Icons.Default.Add, "New Post")
                                }
                                IconButton(onClick = { navController.navigate(Screens.SettingsScreen.route) }) {
                                    Icon(Icons.Default.Menu, "Settings")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                    AnimatedVisibility(visible = isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            bottomBar = {
                if (userId == null) BottomNavigation(BottomNavigationItem.Profile, navController)
            }
        ) { paddingValues ->
            val currentUserState = userState // Biến tạm
            when (currentUserState) {
                is Response.Success -> ProfileContent(
                    navController = navController,
                    user = currentUserState.data,
                    postViewModel = postViewModel,
                    userViewModel = userViewModel,
                    isCurrentUser = userId == null,
                    paddingValues = paddingValues
                )

                is Response.Error -> ErrorToast(currentUserState.message)
                else -> {

                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    navController: NavController,
    user: User,
    postViewModel: PostViewModel,
    userViewModel: UserViewModel,
    isCurrentUser: Boolean,
    paddingValues: PaddingValues
) {
    val currentUserState by userViewModel.currentUser.collectAsState()
    val followingCount by userViewModel.followingCount.collectAsState()
    val followersCount by userViewModel.followersCount.collectAsState()
    val postsIds by userViewModel.postsIds.collectAsState()
    var isFollowing by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = currentUserState) {
        if (currentUserState is Response.Success && !isCurrentUser) {
            val currentUser = (currentUserState as Response.Success<User>).data
            userViewModel.checkIfFollowing(currentUser.userId, user.userId).collect { response ->
                if (response is Response.Success) isFollowing = response.data
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImageWithPlaceholder(
                imageUrl = user.imageUrl,
                modifier = Modifier
                    .size(100.dp) // Tăng kích thước ảnh giống Instagram
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    "Posts",
                    (postsIds as? Response.Success)?.data?.size?.toString() ?: "0"
                )
                StatisticItem(
                    "Followers",
                    (followersCount as? Response.Success)?.data?.toString() ?: "0"
                )
                StatisticItem(
                    "Following",
                    (followingCount as? Response.Success)?.data?.toString() ?: "0"
                )
            }
        }

        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(user.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                user.username,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (user.bio.isNotEmpty()) {
                Text(
                    user.bio,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isCurrentUser) {
            OutlinedButton(
                onClick = { navController.navigate(Screens.EditProfileScreen.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text("Edit Profile", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isFollowing) userViewModel.unfollowUser(user.userId) else userViewModel.followUser(
                            user.userId
                        )
                        isFollowing = !isFollowing
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color(0xFFF5F5F5) else MaterialTheme.colorScheme.primary,
                        contentColor = if (isFollowing) Color.Black else Color.White
                    )
                ) {
                    Text(if (isFollowing) "Following" else "Follow", fontSize = 14.sp)
                }
                OutlinedButton(
                    onClick = {
                        if (currentUserState is Response.Success) {
                            val currentUser = (currentUserState as Response.Success<User>).data
                            navController.navigate("chat/${currentUser.userId}/${user.userId}")
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Message", fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PostsGrid(navController, postsIds, postViewModel)
    }
}

@Composable
private fun StatisticItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun PostsGrid(
    navController: NavController,
    postsIdsState: Response<List<String>>,
    postViewModel: PostViewModel
) {
    val postsState by postViewModel.postsState.collectAsState()
    val loadedPosts = (postsState as? Response.Success)?.data ?: emptyMap()

    LaunchedEffect(key1 = postsIdsState) {
        if (postsIdsState is Response.Success) {
            postsIdsState.data.forEach { postId -> postViewModel.getPostByID(postId) }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (postsIdsState is Response.Success) {
            itemsIndexed(postsIdsState.data, key = { _, postId -> postId }) { _, postId ->
                AnimatedPostThumbnail(postId, loadedPosts, navController)
            }
        }
    }
}

@Composable
private fun AnimatedPostThumbnail(
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
                    .clickable { navController.navigate("postDetail/$postId") },
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun AsyncImageWithPlaceholder(imageUrl: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(if (imageUrl.isNotEmpty()) imageUrl else null)
            .crossfade(true)
            .placeholder(R.drawable.ic_user)
            .error(R.drawable.ic_user)
            .build(),
        contentDescription = "Profile Picture",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun ErrorToast(message: String) {
    val context = LocalContext.current
    LaunchedEffect(key1 = message) {
        android.widget.Toast.makeText(context, "Error: $message", android.widget.Toast.LENGTH_SHORT)
            .show()
    }
}