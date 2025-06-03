package com.arny.allfy.presentation.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screen
import com.arny.allfy.utils.getDataOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel,
    userId: String? = null
) {
    val userState by userViewModel.userState.collectAsState()
    val postState by postViewModel.postState.collectAsState()
    val context = LocalContext.current
    val isCurrentUser = userId == null

    LaunchedEffect(userId) {
        if (userId != null) {
            userViewModel.getUserDetails(userId)
        }
    }

    // Load user stats when user data is available
    LaunchedEffect(userState.currentUserState, userState.otherUserState) {
        val userResponse =
            if (isCurrentUser) userState.currentUserState else userState.otherUserState
        if (userResponse is Response.Success) {
            val user = userResponse.data
            userViewModel.getFollowingCount(user.userId)
            userViewModel.getFollowersCount(user.userId)
            userViewModel.getPostIds(user.userId)
        }
    }

    // Check if following (for other users)
    LaunchedEffect(userState.currentUserState, userState.otherUserState) {
        if (!isCurrentUser &&
            userState.currentUserState is Response.Success &&
            userState.otherUserState is Response.Success
        ) {
            val currentUser = (userState.currentUserState as Response.Success<User>).data
            val otherUser = (userState.otherUserState as Response.Success<User>).data
            userViewModel.checkIfFollowing(currentUser.userId, otherUser.userId)
        }
    }

    LaunchedEffect(postState.deletePostState) {
        when (val deleteState = postState.deletePostState) {
            is Response.Success -> {
                Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()

                val userResponse =
                    if (isCurrentUser) userState.currentUserState else userState.otherUserState
                if (userResponse is Response.Success) {
                    val user = userResponse.data
                    userViewModel.getPostIds(user.userId)
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
                            val username = when (val userResponse =
                                if (isCurrentUser) userState.currentUserState else userState.otherUserState) {
                                is Response.Success -> userResponse.data.username
                                else -> ""
                            }
                            Text(
                                text = username,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            if (!isCurrentUser) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                }
                            }
                        },
                        actions = {
                            if (isCurrentUser) {
                                IconButton(onClick = { navController.navigate(Screen.CreatePostScreen) }) {
                                    Icon(Icons.Default.Add, "New Post")
                                }
                                IconButton(onClick = { navController.navigate(Screen.SettingsScreen) }) {
                                    Icon(Icons.Default.Menu, "Settings")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )

                    // Loading indicator
                    val isLoading = when {
                        isCurrentUser -> userState.currentUserState is Response.Loading
                        else -> userState.otherUserState is Response.Loading ||
                                userState.currentUserState is Response.Loading
                    }

                    // Show delete loading in top bar
                    val isDeleting = postState.deletePostState is Response.Loading

                    AnimatedVisibility(visible = isLoading || isDeleting) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isDeleting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            bottomBar = {
                if (isCurrentUser) BottomNavigation(BottomNavigationItem.Profile, navController)
            }
        ) { paddingValues ->
            ProfileContent(
                navController = navController,
                userState = userState,
                postViewModel = postViewModel,
                userViewModel = userViewModel,
                isCurrentUser = isCurrentUser,
                paddingValues = paddingValues
            )
        }
    }
}

@Composable
private fun ProfileContent(
    navController: NavController,
    userState: com.arny.allfy.presentation.state.UserState,
    postViewModel: PostViewModel,
    userViewModel: UserViewModel,
    isCurrentUser: Boolean,
    paddingValues: PaddingValues
) {
    val userResponse = if (isCurrentUser) userState.currentUserState else userState.otherUserState

    when (userResponse) {
        is Response.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                // Already showing LinearProgressIndicator in TopAppBar
            }
        }

        is Response.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userResponse.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        is Response.Success -> {
            val user = userResponse.data
            val followingCount = (userState.followingCountState as? Response.Success)?.data ?: 0
            val followersCount = (userState.followersCountState as? Response.Success)?.data ?: 0
            val postsIds = (userState.postsIdsState as? Response.Success)?.data ?: emptyList()
            val isFollowing = (userState.checkIfFollowingState as? Response.Success)?.data ?: false

            ProfileDetails(
                navController = navController,
                user = user,
                followingCount = followingCount,
                followersCount = followersCount,
                postsIds = postsIds,
                isFollowing = isFollowing,
                isCurrentUser = isCurrentUser,
                userViewModel = userViewModel,
                postViewModel = postViewModel,
                paddingValues = paddingValues,
                currentUser = if (!isCurrentUser) userState.currentUserState.getDataOrNull() else null
            )
        }

        is Response.Idle -> {
            // Waiting for data
        }
    }
}

@Composable
private fun ProfileDetails(
    navController: NavController,
    user: User,
    followingCount: Int,
    followersCount: Int,
    postsIds: List<String>,
    isFollowing: Boolean,
    isCurrentUser: Boolean,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel,
    paddingValues: PaddingValues,
    currentUser: User?
) {
    var isFollowingState by remember(key1 = isFollowing) { mutableStateOf(isFollowing) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Remember values to avoid unnecessary recomposition
    val rememberedUser = remember(user.userId) { user }
    val rememberedPostsIds = remember(postsIds) { postsIds }

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
                    .size(100.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Posts", postsIds.size.toString())
                StatisticItem("Followers", followersCount.toString())
                StatisticItem("Following", followingCount.toString())
            }
        }

        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(user.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                user.username,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (!user.bio.isNullOrEmpty()) {
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
                onClick = { navController.navigate(Screen.EditProfileScreen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text("Edit Profile", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        } else if (currentUser != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isFollowingState) {
                    OutlinedButton(
                        onClick = {
                            userViewModel.unfollowUser(currentUser.userId, user.userId)
                            isFollowingState = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Following", fontSize = 14.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            userViewModel.followUser(currentUser.userId, user.userId)
                            isFollowingState = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Follow", fontSize = 14.sp)
                    }
                }
                OutlinedButton(
                    onClick = {
                        navController.navigate(
                            Screen.ChatScreen(otherUserId = user.userId)
                        )
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

        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.placehoder_image),
                        contentDescription = null
                    )
                },
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_comment),
                        contentDescription = null
                    )
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        PostsGrid(
            navController = navController,
            postsIds = rememberedPostsIds,
            postViewModel = postViewModel,
            showMediaPosts = selectedTabIndex == 0
        )
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
                Text(
                    text = postsResponse.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        is Response.Success -> {
            val loadedPosts = postsResponse.data.associateBy { it.postID }
            val filteredPostIds = postsIds.filter { postId ->
                val post = loadedPosts[postId]
                if (post != null) {
                    if (showMediaPosts) post.mediaItems.isNotEmpty() else post.mediaItems.isEmpty()
                } else {
                    false
                }
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

        is Response.Idle -> {
            // Waiting for data
        }
    }
}

@Composable
private fun TextOnlyPostItem(
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
                    .clickable {
                        navController.navigate(Screen.PostDetailScreen(postId))
                    },
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