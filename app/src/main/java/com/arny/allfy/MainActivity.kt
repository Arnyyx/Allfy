package com.arny.allfy

import android.Manifest
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.arny.allfy.data.remote.GoogleAuthClient
import com.arny.allfy.presentation.ui.*
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.ui.theme.AllfyTheme
import com.arny.allfy.utils.Screen
import com.google.firebase.auth.FirebaseAuth
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
                Log.d("NotificationPermission", "Permission granted")
            }
        }

    private lateinit var navController: NavHostController
    private lateinit var chatViewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        updateFcmToken(auth, db)
        requestNotificationPermission()
        KeywordExtractor.initialize(applicationContext)

        setContent {
            AllfyTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    navController = rememberNavController()
                    chatViewModel = hiltViewModel()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val userViewModel: UserViewModel = hiltViewModel()
                    val postViewModel: PostViewModel = hiltViewModel()

                    AllfyApp(
                        navController = navController,
                        authViewModel = authViewModel,
                        userViewModel = userViewModel,
                        postViewModel = postViewModel,
                        chatViewModel = chatViewModel,
                        googleAuthClient = googleAuthClient
                    )
                    LaunchedEffect(intent) {
                        handleIntent(intent)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intent.let {
            when {
                it.getBooleanExtra("isIncomingCall", false) -> {
                    val conversationId = it.getStringExtra("conversationId") ?: return@let
                    val callerId = it.getStringExtra("callerId") ?: return@let
                    navController.navigate(
                        Screen.CallScreen(
                            conversationId = conversationId,
                            otherUserId = callerId,
                            isCaller = false,
                        )
                    )
                    Log.d("IncomingCall", "Navigate to incoming call screen")
                }

                it.getBooleanExtra("isChatNotification", false) -> {
                    val conversationId = it.getStringExtra("conversationId") ?: return@let
                    val otherUserId = it.getStringExtra("otherUserId") ?: return@let
                    navController.navigate(
                        Screen.ChatScreen(
                            conversationId = conversationId,
                            otherUserId = otherUserId,
                        )
                    )
                    Log.d("ChatNotification", "Navigate to chat screen")
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
    navController: NavHostController,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel,
    chatViewModel: ChatViewModel,
    googleAuthClient: GoogleAuthClient,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.SplashScreen,
        enterTransition = { fadeIn(animationSpec = tween(1)) },
        exitTransition = { fadeOut(animationSpec = tween(1)) }
    ) {
        composable<Screen.LoginScreen> {
            LoginScreen(navController, authViewModel, googleAuthClient)
        }
        composable<Screen.SignUpScreen> {
            SignUpScreen(navController, authViewModel, googleAuthClient)
        }
        composable<Screen.FeedScreen> {
            FeedScreen(navController, userViewModel, postViewModel, authViewModel)
        }
        composable<Screen.SplashScreen> {
            SplashScreen(navController, authViewModel, userViewModel)
        }
        composable<Screen.ProfileScreen> {
            val args = it.toRoute<Screen.ProfileScreen>()
            ProfileScreen(navController, userViewModel, postViewModel, args.userId)
        }
        composable<Screen.SearchScreen> {
            SearchScreen(navController)
        }
        composable<Screen.EditProfileScreen> {
            EditProfileScreen(navController, userViewModel)
        }
        composable<Screen.CreatePostScreen> {
            CreatePostScreen(navController, postViewModel, userViewModel)
        }
        composable<Screen.SettingsScreen> {
            SettingsScreen(
                navController,
                authViewModel,
                chatViewModel,
                postViewModel,
                userViewModel
            )
        }
        composable<Screen.PostDetailScreen> {
            val postID = it.toRoute<Screen.PostDetailScreen>().postID
            PostDetailScreen(postID, navController, postViewModel, userViewModel, authViewModel)
        }
        composable<Screen.ConversationsScreen> {
            ConversationsScreen(
                navController, userViewModel, chatViewModel
            )
        }
        composable<Screen.ChatScreen> {
            val args = it.toRoute<Screen.ChatScreen>()
            ChatScreen(
                navHostController = navController,
                chatViewModel = chatViewModel,
                userViewModel = userViewModel,
                conversationId = args.conversationId,
                otherUserId = args.otherUserId,
            )
        }
        composable<Screen.CallScreen> {
            val args = it.toRoute<Screen.CallScreen>()
            CallScreen(
                conversationId = args.conversationId,
                isCaller = args.isCaller,
                otherUserId = args.otherUserId,
                userViewModel = userViewModel,
                navController = navController
            )
        }
    }
}