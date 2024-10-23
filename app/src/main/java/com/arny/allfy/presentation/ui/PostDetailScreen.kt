package com.arny.allfy.presentation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.arny.allfy.domain.model.Post
import com.arny.allfy.presentation.common.PostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    post: Post,
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }, actions = {
                    IconButton(onClick = {
                        //TODO more vert
                    }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Settings")
                    }
                }
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                PostItem(
                    post = post, onPostClick = { /* Do nothing, we're already in detail */ })
            }
        }
    }
}