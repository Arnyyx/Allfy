package com.arny.allfy.presentation.ui

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.PostItem
import com.arny.allfy.presentation.common.Toast
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postID: String,
    navController: NavController,
    postViewModel: PostViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    LaunchedEffect(postID) {
        postViewModel.getPostByID(postID)
    }

    val postState by postViewModel.postsState.collectAsState()
    val currentUserState by userViewModel.currentUser.collectAsState()
    val postOwnerState by userViewModel.otherUser.collectAsState()

    LaunchedEffect(postState) {
        if (postState is Response.Success) {
            val post = (postState as Response.Success<Map<String, Post>>).data[postID]
            post?.postOwnerID?.let { userViewModel.getUserById(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (postOwnerState) {
                        is Response.Success -> Text((postOwnerState as Response.Success<User>).data.username)
                        else -> Text("Loading...")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                currentUserState is Response.Loading || postState is Response.Loading || postOwnerState is Response.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                else -> {}
            }

            when (currentUserState) {
                is Response.Error -> {
                    Toast("Error: ${(currentUserState as Response.Error).message}")
                }

                is Response.Success -> {
                    val currentUser = (currentUserState as Response.Success<User>).data
                    when (postState) {
                        is Response.Error -> {
                            Toast("Error: ${(postState as Response.Error).message}")
                        }

                        is Response.Success -> {
                            val post = (postState as Response.Success<Map<String, Post>>).data[postID]
                            if (post == null) {
                                Text(
                                    text = "Post not found",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                when (postOwnerState) {
                                    is Response.Error -> {
                                        Toast("Error loading post owner: ${(postOwnerState as Response.Error).message}")
                                    }

                                    is Response.Success -> {
                                        val postOwner = (postOwnerState as Response.Success<User>).data
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            item {
                                                PostItem(
                                                    initialPost = post,
                                                    currentUser = currentUser,
                                                    navController = navController,
                                                    postOwner = postOwner
                                                )
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                        else -> {}
                    }
                }
                else -> {}
            }
        }
    }
}