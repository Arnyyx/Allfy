package com.arny.allfy.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    postViewModel: PostViewModel,
    userViewModel: UserViewModel
) {
    val postState by postViewModel.postState.collectAsState()
    val userState by userViewModel.userState.collectAsState()

    LaunchedEffect(postID) {
        postViewModel.getPostByID(postID)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    postState.post.postOwnerUsername
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
                postState.isLoadingPost -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    PostItem(
                        post = postState.post,
                        currentUser = userState.currentUser,
                        navController = navController
                    )
                }
            }

        }
    }
}

