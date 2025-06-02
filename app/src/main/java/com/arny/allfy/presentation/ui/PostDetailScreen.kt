package com.arny.allfy.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arny.allfy.presentation.common.PostItem
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.utils.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postID: String,
    navController: NavController,
    postViewModel: PostViewModel,
    userViewModel: UserViewModel,
    authViewModel: AuthViewModel
) {
    val postState by postViewModel.postState.collectAsState()
    val userState by userViewModel.userState.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    val currentUserId = authState.currentUserId

    LaunchedEffect(postID) {
        postViewModel.getPostByID(postID)
        postViewModel.resetLikePostState()
        postViewModel.resetAddCommentState()
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty() && userState.currentUserState is Response.Idle) {
            userViewModel.getCurrentUser(currentUserId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (val postResponse = postState.getPostState) {
                        is Response.Success -> {
                            Text(postResponse.data.postOwnerUsername)
                        }

                        else -> Text("Post")
                    }
                },
                navigationIcon = {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val postResponse = postState.getPostState) {
                is Response.Loading -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }

                is Response.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = postResponse.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is Response.Success -> {
                    when (val userResponse = userState.currentUserState) {
                        is Response.Success -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    PostItem(
                                        post = postResponse.data,
                                        currentUser = userResponse.data,
                                        navController = navController,
                                        postViewModel = postViewModel
                                    )
                                }
                            }
                        }

                        is Response.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        is Response.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Error loading user",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        is Response.Idle -> {}
                    }
                }

                is Response.Idle -> {}
            }
        }
    }
}