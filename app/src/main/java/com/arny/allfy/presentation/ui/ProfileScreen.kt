package com.arny.allfy.presentation.ui

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import com.arny.allfy.presentation.common.BottomNavigation
import com.arny.allfy.presentation.common.BottomNavigationItem
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserState
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel,
    userId: String? = null
) {
    val userState by userViewModel.userState.collectAsState()
    val isCurrentUser = userId == null

    LaunchedEffect(key1 = userId) {
        if (!isCurrentUser) {
            userId?.let { userViewModel.getUserDetails(it) }
        }
        if (isCurrentUser && userState.currentUser.userId.isEmpty()) {
            userViewModel.getCurrentUser("currentUserId")
        }
    }

    LaunchedEffect(userState.currentUser, userState.otherUser) {
        val user = if (isCurrentUser) userState.currentUser else userState.otherUser
        if (user.userId.isNotEmpty()) {
            Log.d("ProfileScreen", "ProfileScreen: $user")
            with(userViewModel) {
                getFollowingCount(user.userId)
                getFollowersCount(user.userId)
                getPostIds(user.userId)
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
                            Text(
                                text = if (isCurrentUser) userState.currentUser.username else userState.otherUser.username,
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
                    AnimatedVisibility(visible = userState.isLoadingCurrentUser || userState.isLoadingOtherUser) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
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
    userState: UserState,
    postViewModel: PostViewModel,
    userViewModel: UserViewModel,
    isCurrentUser: Boolean,
    paddingValues: PaddingValues
) {
    val user = if (isCurrentUser) userState.currentUser else userState.otherUser
    var isFollowing by remember { mutableStateOf(userState.isFollowing) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = userState.currentUser) {
        if (!isCurrentUser && userState.currentUser.userId.isNotEmpty()) {
            userViewModel.checkIfFollowing(userState.currentUser.userId, user.userId)
        }
    }

    LaunchedEffect(userState.isFollowing) {
        isFollowing = userState.isFollowing
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
                    .size(100.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Posts", userState.postsIds.size.toString())
                StatisticItem("Followers", userState.followersCount.toString())
                StatisticItem("Following", userState.followingCount.toString())
            }
        }

        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(user.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                user.username,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            if (user.bio?.isNotEmpty() == true) {
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
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isFollowing) {
                            userViewModel.unfollowUser(userState.currentUser.userId, user.userId)
                        } else {
                            userViewModel.followUser(userState.currentUser.userId, user.userId)
                        }
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
                        navController.navigate(
                            Screen.ChatScreen(
                                currentUserId = userState.currentUser.userId,
                                otherUserId = userState.otherUser.userId
                            )
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
            postsIds = userState.postsIds,
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
        postViewModel.getPostsByIds(postsIds)
    }
    val postState by postViewModel.postState.collectAsState()
    val loadedPosts = postState.posts.associateBy { it.postID }

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
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
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

@Composable
private fun ErrorToast(message: String) {
    val context = LocalContext.current
    LaunchedEffect(key1 = message) {
        android.widget.Toast.makeText(context, "Error: $message", android.widget.Toast.LENGTH_SHORT)
            .show()
    }
}