package com.arny.allfy.presentation.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.NavigationBarItem
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.arny.allfy.utils.Screens

enum class BottomNavigationItem(val icon: ImageVector, val route: String) {
    Feed(Icons.Default.Home, Screens.FeedScreen.route),
    Search(Icons.Default.Search, Screens.SearchScreen.route),
    Profile(Icons.Default.Person, Screens.ProfileScreen.route)
}

@Composable
fun BottomNavigation(selectedItem: BottomNavigationItem, navController: NavController) {
    NavigationBar(
        Modifier.height(60.dp)
    ) {
        for (item in BottomNavigationItem.entries) {
            NavigationBarItem(
                selected = selectedItem == item,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(Screens.FeedScreen.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        item.icon, null, modifier = Modifier.size(24.dp),
                        tint = if (selectedItem == item) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            )
        }

    }
}