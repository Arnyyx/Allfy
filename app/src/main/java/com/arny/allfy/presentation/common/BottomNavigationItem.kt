package com.arny.allfy.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.arny.allfy.utils.Screens

enum class BottomNavigationItem(val icon: ImageVector, val route: String, val label: String) {
    Feed(Icons.Default.Home, Screens.FeedScreen.route, "Home"),
    Search(Icons.Default.Search, Screens.SearchScreen.route, "Search"),
    Create(Icons.Default.Add, Screens.CreatePostScreen.route, "Create"), // Thêm nút Create
    Profile(Icons.Default.Person, Screens.ProfileScreen.route, "Profile")
}

@Composable
fun BottomNavigation(
    selectedItem: BottomNavigationItem,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .height(64.dp)
            .background(Color.Transparent),
        containerColor = Color.Transparent,
        tonalElevation = 4.dp
    ) {
        BottomNavigationItem.entries.forEach { item ->
            val isSelected = selectedItem == item
            NavigationBarItem(
                selected = isSelected,
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
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200))
                    ) {
                        BadgedBox(
                            badge = {
                                // Có thể thêm badge cho thông báo sau này
                            }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier
                                    .size(if (isSelected) 28.dp else 24.dp)
                                    .clip(CircleShape),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                alwaysShowLabel = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationPreview() {
    val navController = rememberNavController()
    BottomNavigation(
        selectedItem = BottomNavigationItem.Feed,
        navController = navController
    )
}