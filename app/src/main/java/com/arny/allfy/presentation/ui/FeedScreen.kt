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
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.BottomNavigation
import com.arny.allfy.presentation.common.BottomNavigationItem
import com.arny.allfy.presentation.common.PostItem
import com.arny.allfy.utils.Screen
import com.arny.allfy.utils.Response
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshState
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay

@Composable
fun FeedScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel,
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
        }
    }

    when (val currentUserResponse = userState.currentUserState) {
        is Response.Loading -> LoadingScreenWithNavigation(navController)
        is Response.Error -> ErrorToast(currentUserResponse.message)
        is Response.Success -> {
            FeedContent(
                currentUser = currentUserResponse.data,
                postViewModel = postViewModel,
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
    navController: NavController
) {
    val state by postViewModel.postState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(currentUser.userId) {
        if (state.feedPostsState is Response.Idle) {
            postViewModel.getFeedPosts(currentUser.userId)
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
                if (state.feedPostsState is Response.Loading) {
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
            when (val feedPostsResponse = state.feedPostsState) {
                is Response.Loading -> {}
                is Response.Error -> ErrorScreen(feedPostsResponse.message)
                is Response.Success -> {
                    val posts = feedPostsResponse.data
                    if (posts.isEmpty()) {
                        EmptyScreen("No posts available")
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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

                is Response.Idle -> {}
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

@Composable
private fun ErrorScreen(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ErrorToast(message: String) {
    var showToast by remember { mutableStateOf(true) }
    if (showToast) {
        Toast.makeText(LocalContext.current, message, Toast.LENGTH_SHORT).show()
        LaunchedEffect(Unit) {
            delay(2000)
            showToast = false
        }
    }
}