package com.arny.allfy.presentation.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arny.allfy.R
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.BottomNavigation
import com.arny.allfy.presentation.common.BottomNavigationItem
import com.arny.allfy.presentation.common.PostItem
import com.arny.allfy.presentation.common.Toast
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screens

@Composable
fun FeedScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel,
) {
    LaunchedEffect(Unit) {
        userViewModel.getCurrentUser()
    }
    val currentUser by userViewModel.currentUser.collectAsState()

    when (currentUser) {
        is Response.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is Response.Success -> {
            val user = (currentUser as Response.Success<User>).data
            LoadPosts(user, postViewModel, navController)
        }

        is Response.Error -> {
            Toast("Error: ${(currentUser as Response.Error).message}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadPosts(
    currentUser: User,
    postViewModel: PostViewModel,
    navController: NavController
) {
    val state by postViewModel.getFeedPostsState.collectAsState()

    LaunchedEffect(Unit) {
        postViewModel.getFeedPosts(currentUser.userID)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Allfy") },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screens.ConversationsScreen.route)
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_message),
                            contentDescription = "Messages"
                        )
                    }
                }
            )
        },
        bottomBar = { BottomNavigation(BottomNavigationItem.Feed, navController) }
    ) { paddingValues ->
        when {
            state.isLoading && state.posts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error.isNotBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.error, color = Color.Red)
                }
            }

            state.posts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No posts available")
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(
                        items = state.posts,
                        key = { post -> post.postID }
                    ) { post ->
                        PostItem(
                            initialPost = post,
                            currentUser = currentUser,
                            navController = navController
                        )
                    }

                    if (!state.endReached) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                            LaunchedEffect(Unit) {
                                postViewModel.getFeedPosts(currentUser.userID)
                            }
                        }
                    }
                }
            }
        }
    }
}
