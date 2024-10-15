package com.arny.allfy.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.arny.allfy.presentation.common.BottomNavigationItem
import com.arny.allfy.presentation.common.BottomNavigation
import com.arny.allfy.presentation.common.Toast
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.utils.Response

@Composable
fun FeedScreen(navController: NavController) {
    val postViewModel: PostViewModel = hiltViewModel()
    postViewModel.getAllPosts("123")
    when (val response = postViewModel.postData.value) {
        is Response.Loading -> {
            CircularProgressIndicator(
            )
        }

        is Response.Success -> {
            response.data.forEach {
            }
        }

        is Response.Error -> {
            Toast(response.message)
        }

    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Feed Screen")
        }
        BottomNavigation(BottomNavigationItem.Feed, navController)
    }
}