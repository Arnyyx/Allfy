package com.arny.allfy.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.arny.allfy.presentation.common.BottomNavigationItem
import com.arny.allfy.presentation.common.BottomNavigation

@Composable
fun FeedScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Feed Screen")
        }
        BottomNavigation( BottomNavigationItem.Feed,navController)
    }
}