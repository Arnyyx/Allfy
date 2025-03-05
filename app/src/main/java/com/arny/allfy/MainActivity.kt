package com.arny.allfy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arny.allfy.data.remote.GoogleAuthClient
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.ui.*
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.ui.theme.AllfyTheme
import com.arny.allfy.utils.Screens
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var googleAuthClient: GoogleAuthClient

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Quyền được cấp
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        updateFcmToken(auth, db)

        requestNotificationPermission()

        setContent {
            AllfyTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val userViewModel: UserViewModel = hiltViewModel()
                    val postViewModel: PostViewModel = hiltViewModel()
                    val chatViewModel: ChatViewModel = hiltViewModel()
                    AllfyApp(
                        navController,
                        authViewModel,
                        userViewModel,
                        postViewModel,
                        chatViewModel,
                        googleAuthClient
                    )
                }
            }
        }
    }

    private fun updateFcmToken(auth: FirebaseAuth, db: FirebaseFirestore) {
        val preferences = getSharedPreferences("FCMPrefs", MODE_PRIVATE)
        val editor = preferences.edit()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener

            val newToken = task.result
            val storedToken = preferences.getString("fcmToken", null)
            val user = auth.currentUser

            if (user != null && newToken != storedToken) {
                editor.putString("fcmToken", newToken).apply()
                val userId = user.uid
                val userDocRef = db.collection("users").document(userId)
                userDocRef.get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        userDocRef.update("fcmToken", newToken)
                    } else {
                        val username =
                            user.email?.substringBefore("@")?.replace(".", "") ?: "user_$userId"
                        val name = user.displayName ?: "User ${userId.take(5)}"
                        val userData = mapOf(
                            "userId" to userId,
                            "username" to username,
                            "name" to name,
                            "email" to user.email,
                            "imageUrl" to "",
                            "bio" to "",
                            "fcmToken" to newToken
                        )
                        userDocRef.set(userData)
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("NotificationPermission", "Permission already granted")
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Log.d("NotificationPermission", "Showing rationale")
                    AlertDialog.Builder(this)
                        .setTitle("Notification Permission Needed")
                        .setMessage("This app needs permission to send notifications for new messages.")
                        .setPositiveButton("OK") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                else -> {
                    Log.d("NotificationPermission", "Requesting permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
    chatViewModel: ChatViewModel,
    googleAuthClient: GoogleAuthClient
) {
    NavHost(
        navController = navHostController,
        startDestination = Screens.SplashScreen.route,
        enterTransition = { fadeIn(animationSpec = tween(1)) },
        exitTransition = { fadeOut(animationSpec = tween(1)) }
    ) {
        composable(Screens.LoginScreen.route) {
            LoginScreen(navHostController, authViewModel, googleAuthClient)
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
            SettingsScreen(
                navHostController,
                authViewModel,
                chatViewModel,
                postViewModel,
                userViewModel
            )
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
                userViewModel,
                chatViewModel
            )
        }
        composable(
            route = "chat/{currentUserId}/{otherUserId}",
            arguments = listOf(
                navArgument("currentUserId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val currentUserId = backStackEntry.arguments?.getString("currentUserId")
            val otherUserId = backStackEntry.arguments?.getString("otherUserId")
            requireNotNull(currentUserId) { "currentUserId parameter wasn't found" }
            requireNotNull(otherUserId) { "otherUserId parameter wasn't found" }
            ChatScreen(
                navHostController = navHostController,
                chatViewModel = chatViewModel,
                userViewModel = userViewModel,
                currentUserId = currentUserId,
                otherUserId = otherUserId
            )
        }
    }
}