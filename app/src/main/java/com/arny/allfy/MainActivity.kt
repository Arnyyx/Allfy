package com.arny.allfy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.ui.ChatScreen
import com.arny.allfy.presentation.ui.ConversationsScreen
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
import com.arny.allfy.presentation.viewmodel.ConversationsViewModel
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
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
                    val userViewModel: UserViewModel = hiltViewModel()
                    val postViewModel: PostViewModel = hiltViewModel()
                    val conversationsViewModel: ConversationsViewModel = hiltViewModel()
                    AllfyApp(
                        navController,
                        authViewModel,
                        userViewModel,
                        postViewModel,
                        conversationsViewModel
                    )
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
    postViewModel: PostViewModel,
    conversationsViewModel: ConversationsViewModel
) {
    NavHost(
        navController = navHostController,
        startDestination = Screens.SplashScreen.route,
        enterTransition = {
            fadeIn(animationSpec = tween(1))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(1))
        }
    ) {
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
        composable("profile/{userID}") { backStackEntry ->
            val userID = backStackEntry.arguments?.getString("userID") ?: ""
            ProfileScreen(navHostController, userViewModel, postViewModel, userID)
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
        composable(Screens.ConversationsScreen.route) {
            ConversationsScreen(
                navHostController,
                conversationsViewModel,
                userViewModel
            ) { participantId ->
                navHostController.navigate("chat/$participantId")
            }
        }
        composable(
            route = "chat/{participantId}",
            arguments = listOf(
                navArgument("participantId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val participantId = backStackEntry.arguments?.getString("participantId")
            requireNotNull(participantId) { "participantId parameter wasn't found" }
            val user = User(
                userID = participantId
            )

            ChatScreen(
                viewModel = hiltViewModel(),
                otherUser = user,
                onBackClick = { navHostController.popBackStack() }
            )
        }
    }
}