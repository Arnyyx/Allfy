package com.arny.allfy.presentation.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    val currentUserId = authState.currentUserId

    LaunchedEffect(postID) {
        postViewModel.getPostByID(postID)
        postViewModel.resetLikePostState()
        postViewModel.resetAddCommentState()
        postViewModel.resetDeletePostState()
    }

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty() && userState.currentUserState is Response.Idle) {
            userViewModel.getCurrentUser(currentUserId)
        }
    }

    // Handle delete success
    LaunchedEffect(postState.deletePostState) {
        when (val deleteState = postState.deletePostState) {
            is Response.Success -> {
                Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
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

    Scaffold(
        topBar = {
            Column {
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

                // Show loading indicator for delete operation
                if (postState.deletePostState is Response.Loading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
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