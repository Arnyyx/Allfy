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
import com.arny.allfy.presentation.common.Toast
import com.arny.allfy.presentation.ui.CreatePostScreen
import com.arny.allfy.presentation.ui.EditProfileScreen
import com.arny.allfy.presentation.ui.FeedScreen
import com.arny.allfy.presentation.ui.LoginScreen
import com.arny.allfy.presentation.ui.PostDetailScreen
import com.arny.allfy.presentation.ui.ProfileScreen
import com.arny.allfy.presentation.ui.SearchScreen
import com.arny.allfy.presentation.ui.SettingsScreen
import com.arny.allfy.presentation.ui.SignUpScreen
import com.arny.allfy.presentation.ui.SplashScreen
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.ui.theme.AllfyTheme
import com.arny.allfy.utils.Response
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
                    val userViewModel: UserViewModel = hiltViewModel()
                    val postViewModel: PostViewModel = hiltViewModel()
                    AllfyApp(navController, authViewModel, userViewModel, postViewModel)
                }
            }
        }
    }
}

@Composable
fun AllfyApp(
    navHostController: NavHostController,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel
) {
    NavHost(navController = navHostController, startDestination = Screens.SplashScreen.route) {
        composable(Screens.LoginScreen.route) {
            LoginScreen(navHostController, authViewModel)
        }
        composable(Screens.SignUpScreen.route) {
            SignUpScreen(navHostController, authViewModel)
        }
        composable(Screens.FeedScreen.route) {
            FeedScreen(navHostController, userViewModel, postViewModel)
        }
        composable(Screens.SplashScreen.route) {
            SplashScreen(navController = navHostController, authViewModel)
        }
        composable(Screens.ProfileScreen.route) {
            ProfileScreen(navHostController, userViewModel, postViewModel)
        }
        composable(Screens.SearchScreen.route) {
            SearchScreen(navHostController)
        }
        composable(Screens.EditProfileScreen.route) {
            EditProfileScreen(onBackClick = { navHostController.popBackStack() }, userViewModel)
        }
        composable(Screens.CreatePostScreen.route) {
            CreatePostScreen(navHostController, postViewModel, userViewModel)
        }
        composable(Screens.SettingsScreen.route) {
            SettingsScreen(navHostController, authViewModel)
        }
        composable("postDetail/{postID}") { backStackEntry ->
            val postID = backStackEntry.arguments?.getString("postID")
            if (postID != null) {
                PostDetailScreen(postID, navHostController, postViewModel, userViewModel)
            }
        }
    }
}