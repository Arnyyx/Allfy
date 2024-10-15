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
fun SearchScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(2f)) {
            Text(text = "Search Screen")
        }
        BottomNavigation( BottomNavigationItem.Search,navController)
    }
}