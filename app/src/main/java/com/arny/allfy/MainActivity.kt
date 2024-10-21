package com.arny.allfy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arny.allfy.presentation.ui.CreatePostScreen
import com.arny.allfy.presentation.ui.EditProfileScreen
import com.arny.allfy.presentation.ui.FeedScreen
import com.arny.allfy.presentation.ui.LoginScreen
import com.arny.allfy.presentation.ui.ProfileScreen
import com.arny.allfy.presentation.ui.SearchScreen
import com.arny.allfy.presentation.ui.SettingsScreen
import com.arny.allfy.presentation.ui.SignUpScreen
import com.arny.allfy.presentation.ui.SplashScreen
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.ui.theme.AllfyTheme
import com.arny.allfy.utils.Screens
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AllfyTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    AllfyApp(navController, authViewModel)
                }
            }
        }
    }
}

@Composable
fun AllfyApp(navHostController: NavHostController, authViewModel: AuthViewModel) {
    NavHost(navController = navHostController, startDestination = Screens.SplashScreen.route) {
        composable(Screens.LoginScreen.route) {
            LoginScreen(navHostController, authViewModel)
        }
        composable(Screens.SignUpScreen.route) {
            SignUpScreen(navHostController, authViewModel)
        }
        composable(Screens.FeedScreen.route) {
            FeedScreen(navHostController)
        }
        composable(Screens.SplashScreen.route) {
            SplashScreen(navController = navHostController, authViewModel)
        }
        composable(Screens.ProfileScreen.route) {
            ProfileScreen(navHostController)
        }
        composable(Screens.SearchScreen.route) {
            SearchScreen(navHostController)
        }
        composable(Screens.EditProfileScreen.route) {
            EditProfileScreen(onBackClick = { navHostController.popBackStack() }, )
        }
        composable(Screens.CreatePostScreen.route) {
            CreatePostScreen(onBackClick = { navHostController.popBackStack() })
        }
        composable(Screens.SettingsScreen.route) {
            SettingsScreen(navHostController, authViewModel)
        }
    }
}

//TODO change app icon, receive posts


