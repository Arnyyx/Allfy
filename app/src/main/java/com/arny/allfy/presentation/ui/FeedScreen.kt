package com.arny.allfy.presentation.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.arny.allfy.R
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.Story
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.BottomNavigation
import com.arny.allfy.presentation.common.BottomNavigationItem
import com.arny.allfy.presentation.common.PostItem
import com.arny.allfy.presentation.components.StoryRingAvatar
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.StoryViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screen
import kotlinx.coroutines.delay

@Composable
fun FeedScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel,
    storyViewModel: StoryViewModel,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val userState by userViewModel.userState.collectAsState()

    val currentUserId = authState.currentUserId

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated && currentUserId.isEmpty()) {
            authViewModel.getCurrentUserId()
        }
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty() && userState.currentUserState is Response.Idle) {
            userViewModel.getCurrentUser(currentUserId)
            userViewModel.getFollowings(currentUserId)
        }
    }

    when (val currentUserResponse = userState.currentUserState) {
        is Response.Loading -> LoadingScreenWithNavigation(navController)
        is Response.Error -> {
            Toast.makeText(LocalContext.current, currentUserResponse.message, Toast.LENGTH_SHORT)
                .show()
        }

        is Response.Success -> {
            FeedContent(
                currentUser = currentUserResponse.data,
                postViewModel = postViewModel,
                storyViewModel = storyViewModel,
                userViewModel = userViewModel,
                navController = navController
            )
        }

        is Response.Idle -> LoadingScreenWithNavigation(navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedContent(
    currentUser: User,
    postViewModel: PostViewModel,
    storyViewModel: StoryViewModel,
    userViewModel: UserViewModel,
    navController: NavController
) {
    val postState by postViewModel.postState.collectAsState()
    val userState by userViewModel.userState.collectAsState()
    val storyState by storyViewModel.storyState.collectAsState()
    val listState = rememberLazyListState()

    // Load posts
    LaunchedEffect(currentUser.userId, "posts") {
        if (postState.feedPostsState is Response.Idle) {
            postViewModel.getFeedPosts(currentUser.userId)
        }
    }

    // Load stories
    LaunchedEffect(currentUser.userId, userState.followingsState, "stories") {
        if (userState.followingsState is Response.Success) {
            val followings = (userState.followingsState as Response.Success).data
            val followingIds = followings.map { it.userId }
            if (followingIds.isNotEmpty()) {
                if (storyState.storiesByUsersState is Response.Idle) {
                    storyViewModel.getStoriesByUserIds(followingIds)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            fontFamily = FontFamily(Font(R.font.quetine, FontWeight.Normal)),
                            text = "Allfy",
                            fontSize = 30.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.ConversationsScreen) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_message),
                                contentDescription = "Messages",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (postState.feedPostsState is Response.Loading || storyState.storiesByUsersState is Response.Loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        bottomBar = {
            BottomNavigation(
                selectedItem = BottomNavigationItem.Feed,
                navController = navController,
                onRefresh = {
                    postViewModel.resetFeedPostsState()
                    postViewModel.getFeedPosts(currentUser.userId)
                    storyViewModel.resetStoriesByUsersState()
                    userViewModel.resetFollowingsState()
                    userViewModel.getFollowings(currentUser.userId)
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    when (val storiesResponse = storyState.storiesByUsersState) {
                        is Response.Loading -> {}
                        is Response.Idle -> {}
                        is Response.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = storiesResponse.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        is Response.Success -> {
                            val stories = storiesResponse.data
                            if (stories.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No stories available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                val userIdsWithStories = stories.groupBy { it.userID }.keys.toList()
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(
                                        items = stories.groupBy { it.userID }.entries.toList(),
                                        key = { entry: Map.Entry<String, List<Story>> -> entry.key }
                                    ) { entry: Map.Entry<String, List<Story>> ->
                                        val userId = entry.key
                                        val userStories = entry.value
                                        val user = userStories.firstOrNull()?.storyOwner
                                        if (user != null) {
                                            StoryRingAvatar(
                                                imageUrl = user.imageUrl,
                                                hasStory = true,
                                                size = 80.dp,
                                                strokeWidth = 2.dp,
                                                onClick = {
                                                    navController.navigate(
                                                        Screen.StoryViewerScreen(
                                                            userId = user.userId,
                                                            isCurrentUser = user.userId == currentUser.userId,
                                                            userIdsWithStories = userIdsWithStories
                                                        )
                                                    )
                                                    storyViewModel.logStoryView(
                                                        currentUser.userId,
                                                        userStories.first().storyID
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Posts Section
                item {
                    when (val feedPostsResponse = postState.feedPostsState) {
                        is Response.Loading -> {}
                        is Response.Idle -> {}
                        is Response.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = feedPostsResponse.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        is Response.Success -> {
                            val posts = feedPostsResponse.data
                            if (posts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No posts available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
                // Posts Content
                if (postState.feedPostsState is Response.Success) {
                    val posts = (postState.feedPostsState as Response.Success).data
                    itemsIndexed(
                        items = posts,
                        key = { _, post -> post.postID }
                    ) { _, post ->
                        AnimatedPostItem(
                            post = post,
                            currentUser = currentUser,
                            postViewModel = postViewModel,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedPostItem(
    post: Post,
    currentUser: User,
    postViewModel: PostViewModel,
    navController: NavController
) {
    LaunchedEffect(post.postID) {
        postViewModel.logPostView(currentUser.userId, post.postID)
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
    ) {
        PostItem(
            post = post,
            currentUser = currentUser,
            navController = navController,
            postViewModel = postViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingScreenWithNavigation(navController: NavController) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            fontFamily = FontFamily(Font(R.font.quetine, FontWeight.Normal)),
                            text = "Allfy",
                            fontSize = 30.sp
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        bottomBar = { BottomNavigation(BottomNavigationItem.Feed, navController) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}