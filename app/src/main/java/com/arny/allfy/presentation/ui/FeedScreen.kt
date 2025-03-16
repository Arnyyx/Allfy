package com.arny.allfy.presentation.ui

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
import com.arny.allfy.presentation.common.Toast
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screens
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay

@Composable
fun FeedScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel
) {
    LaunchedEffect(Unit) {
        userViewModel.getCurrentUser()
    }
    val currentUser by userViewModel.currentUser.collectAsState()

    when (currentUser) {
        is Response.Loading -> LoadingScreenWithNavigation(navController)
        is Response.Success -> {
            val user = (currentUser as Response.Success<User>).data
            FeedContent(
                currentUser = user,
                postViewModel = postViewModel,
                navController = navController
            )
        }

        is Response.Error -> ErrorToast((currentUser as Response.Error).message)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedContent(
    currentUser: User,
    postViewModel: PostViewModel,
    navController: NavController
) {
    val state by postViewModel.getFeedPostsState.collectAsState()
    val users by postViewModel.users.collectAsState()
    val listState = rememberLazyListState()
    val refreshState =
        rememberSwipeRefreshState(isRefreshing = state.isLoading && state.posts.isEmpty())

    LaunchedEffect(Unit) {
        if (state.posts.isEmpty() && !state.isLoading) {
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
                        IconButton(onClick = {
                            navController.navigate(Screens.ConversationsScreen.route)
                        }) {
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
                if (state.isLoading && state.posts.isEmpty()) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        bottomBar = { BottomNavigation(BottomNavigationItem.Feed, navController) }
    ) { paddingValues ->
        SwipeRefresh(
            state = refreshState,
            onRefresh = { postViewModel.getFeedPosts(currentUser.userId, forceRefresh = true) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when {
                    state.error.isNotBlank() -> ErrorScreen(state.error)
                    state.posts.isEmpty() && !state.isLoading -> EmptyScreen("No posts available")
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = state.posts,
                                key = { _, post -> post.postID }
                            ) { index, post ->
                                val postOwner = users[post.postOwnerID]
                                AnimatedPostItem(
                                    post = post,
                                    currentUser = currentUser,
                                    postOwner = postOwner,
                                    navController = navController,
                                    postViewModel = postViewModel
                                )
                            }

                            if (!state.endReached && state.posts.isNotEmpty()) {
                                item {
                                    LoadingMorePosts(
                                        isLoading = state.isLoading,
                                        onLoadMore = {
                                            if (!state.isLoading) {
                                                postViewModel.getFeedPosts(currentUser.userId)
                                            }
                                        }
                                    )
                                }
                            }
                        }
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
    postOwner: User?,
    navController: NavController,
    postViewModel: PostViewModel,
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
    ) {
        if (postOwner != null) {
            PostItem(
                initialPost = post,
                currentUser = currentUser,
                postOwner = postOwner,
                navController = navController,
                postViewModel = postViewModel
            )
        } else {
        }
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
private fun LoadingMorePosts(isLoading: Boolean, onLoadMore: () -> Unit) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            LinearProgressIndicator()
        }
    }
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            onLoadMore()
        }
    }
}

@Composable
private fun ErrorToast(message: String) {
    var showToast by remember { mutableStateOf(true) }
    if (showToast) {
        Toast(message)
        LaunchedEffect(Unit) {
            delay(2000)
            showToast = false
        }
    }
}