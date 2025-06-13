package com.arny.allfy.presentation.components

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.arny.allfy.R
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.AsyncImageWithPlaceholder
import com.arny.allfy.presentation.state.UserState
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screen
import com.arny.allfy.utils.getDataOrNull

@Composable
fun ProfileContent(
    navController: NavController,
    userState: UserState,
    postViewModel: PostViewModel,
    userViewModel: UserViewModel,
    isCurrentUser: Boolean,
    paddingValues: PaddingValues,
    onAvatarClick: () -> Unit,
    onCreateStoryClick: () -> Unit

) {
    val userResponse = if (isCurrentUser) userState.currentUserState else userState.otherUserState
    var showBottomSheet by remember { mutableStateOf(false) }

    when (userResponse) {
        is Response.Loading -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
            (userState.currentUserState as? Response.Success)?.data?.let {
                ProfileDetails(
                    navController = navController,
                    user = userResponse.data,
                    followingCount = (userState.followingCountState as? Response.Success)?.data
                        ?: 0,
                    followersCount = (userState.followersCountState as? Response.Success)?.data
                        ?: 0,
                    postsIds = (userState.postsIdsState as? Response.Success)?.data ?: emptyList(),
                    isFollowing = (userState.checkIfFollowingState as? Response.Success)?.data
                        ?: false,
                    isCurrentUser = isCurrentUser,
                    userViewModel = userViewModel,
                    postViewModel = postViewModel,
                    paddingValues = paddingValues,
                    currentUser = if (!isCurrentUser) userState.currentUserState.getDataOrNull() else null,
                    onAvatarClick = onAvatarClick,
                    onCreateStoryClick = onCreateStoryClick
                )
            }
        }

        is Response.Idle -> {}
    }

    if (showBottomSheet) {
        val user = (userResponse as? Response.Success)?.data
        user?.let {
            ProfileBottomSheet(
                hasStories = it.hasStory,
                onDismiss = { showBottomSheet = false },
                onViewAvatar = { onAvatarClick() },
                onViewStories = {
                    navController.navigate(
                        Screen.StoryViewerScreen(
                            it.userId,
                            isCurrentUser
                        )
                    )
                },
            )
        }
    }
}

@Composable
fun ProfileDetails(
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
    currentUser: User?,
    onAvatarClick: () -> Unit,
    onCreateStoryClick: () -> Unit
) {
    var isFollowingState by remember(key1 = isFollowing) { mutableStateOf(isFollowing) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
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
            Box {
                StoryRingAvatar(
                    imageUrl = user.imageUrl,
                    hasStory = user.hasStory,
                    size = 120.dp,
                    onClick = onAvatarClick
                )
                if (isCurrentUser) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onCreateStoryClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Posts", postsIds.size.toString())
                StatisticItem(
                    label = "Followers",
                    value = followersCount.toString(),
                    onClick = { navController.navigate(Screen.FollowScreen(user.userId, 0)) }
                )
                StatisticItem(
                    label = "Following",
                    value = followingCount.toString(),
                    onClick = { navController.navigate(Screen.FollowScreen(user.userId, 1)) }
                )
            }
        }
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(user.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                        shape = RoundedCornerShape(12.dp)
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Follow", fontSize = 14.sp)
                    }
                }
                OutlinedButton(
                    onClick = { navController.navigate(Screen.ChatScreen(otherUserId = user.userId)) },
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
                        painterResource(id = R.drawable.placehoder_image),
                        contentDescription = null
                    )
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                icon = {
                    Icon(
                        painterResource(id = R.drawable.ic_comment),
                        contentDescription = null
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        PostsGrid(
            navController = navController,
            postsIds = postsIds,
            postViewModel = postViewModel,
            showMediaPosts = selectedTabIndex == 0
        )
    }
}

@Composable
fun StatisticItem(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun StoryRingAvatar(
    imageUrl: String?,
    hasStory: Boolean,
    size: Dp = 120.dp,
    strokeWidth: Dp = 3.dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (hasStory) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidthPx = strokeWidth.toPx()
                val radius = (size.toPx() - strokeWidthPx) / 2

                val gradient = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFE91E63),
                        Color(0xFFFF5722),
                        Color(0xFFFFC107),
                        Color(0xFFE91E63)
                    ),
                    center = center
                )

                drawCircle(
                    brush = gradient,
                    radius = radius,
                    style = Stroke(width = strokeWidthPx),
                    center = center
                )
            }
        }

        if (imageUrl != null) {
            AsyncImageWithPlaceholder(
                imageUrl = imageUrl,
                modifier = Modifier
                    .size(size - strokeWidth * 2 - 4.dp)
                    .clip(CircleShape)
                    .clickable { onClick() }
            )
        }
    }
}