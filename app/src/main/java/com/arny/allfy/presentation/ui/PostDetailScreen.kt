package com.arny.allfy.presentation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    LaunchedEffect(Unit) {
        userViewModel.getCurrentUser()
        postViewModel.getPostByID(postID)
    }

    when (userViewModel.getCurrentUser.value) {
        is Response.Loading -> CircularProgressIndicator()
        is Response.Error -> {
            Toast("Error: ${(userViewModel.getCurrentUser.value as Response.Error).message}")
        }

        is Response.Success -> {
            val currentUser = (userViewModel.getCurrentUser.value as Response.Success<User>).data
            val postState by postViewModel.postsState.collectAsState()
            when (postState) {
                Response.Loading -> {
                    CircularProgressIndicator()
                }

                is Response.Error -> {
                    Toast("Error: ${(postState as Response.Error).message}")
                }

                is Response.Success -> {
                    val post =
                        (postState as? Response.Success<Map<String, Post>>)?.data?.get(postID)
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Post Detail") },
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
                        if (post != null) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                item {
                                    PostItem(
                                        post = post,
                                        currentUser = currentUser,
                                        navController = navController
                                    )
                                }
                            }
                        } else {
                            Text("Post not found")
                        }
                    }
                }
            }
        }
    }
}
