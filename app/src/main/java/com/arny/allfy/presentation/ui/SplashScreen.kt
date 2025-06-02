package com.arny.allfy.presentation.ui

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.arny.allfy.R
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screen
import com.arny.allfy.utils.getDataOrNull

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel
) {
    val scale = remember { Animatable(0f) }
    val authState by authViewModel.authState.collectAsState()
    val userState by userViewModel.userState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val intent = activity?.intent

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 0.5f,
            animationSpec = tween(
                durationMillis = 1,
                easing = { it * it }
            )
        )
    }

    LaunchedEffect(authState.isAuthenticated) {
        Log.d("SplashScreen", "Is authenticated: ${authState.isAuthenticated}")
        if (authState.isAuthenticated && authState.currentUserId.isEmpty()) {
            authViewModel.getCurrentUserId()
        }
    }

    // Load current user when ID is available
    LaunchedEffect(authState.currentUserId) {
        if (authState.currentUserId.isNotEmpty()) {
            Log.d("SplashScreen", "Current user ID: ${authState.currentUserId}")
            userViewModel.getCurrentUser(authState.currentUserId)
        }
    }

    // Handle navigation based on user state
    LaunchedEffect(userState.currentUserState, authState.isAuthenticated) {
        when (val currentUserResponse = userState.currentUserState) {
            is Response.Success -> {
                Log.d("SplashScreen", userState.currentUserState.getDataOrNull().toString())
                intent?.let {
                    when {
                        it.getBooleanExtra("isChatNotification", false) -> {
                            val conversationId = it.getStringExtra("conversationId") ?: return@let
                            val otherUserId = it.getStringExtra("otherUserId") ?: return@let
                            navController.navigate(
                                Screen.ChatScreen(
                                    conversationId = conversationId,
                                    otherUserId = otherUserId
                                )
                            )
                        }

                        it.getBooleanExtra("isIncomingCall", false) -> {
                            val conversationId = it.getStringExtra("conversationId") ?: return@let
                            val callerId = it.getStringExtra("callerId") ?: return@let
                            navController.navigate(
                                Screen.CallScreen(
                                    conversationId = conversationId,
                                    isCaller = false,
                                    otherUserId = callerId
                                )
                            )
                        }

                        else -> {
                            navController.navigate(Screen.FeedScreen) {
                                Log.d("SplashScreen", "Navigate to feed screen")
                                popUpTo(Screen.SplashScreen) { inclusive = true }
                            }
                        }
                    }
                } ?: run {
                    navController.navigate(Screen.FeedScreen) {
                        popUpTo(Screen.SplashScreen) { inclusive = true }
                    }
                }
            }

            is Response.Error -> {
                Toast.makeText(context, currentUserResponse.message, Toast.LENGTH_SHORT).show()
                // Navigate to login if user load fails
                navController.navigate(Screen.LoginScreen) {
                    popUpTo(Screen.SplashScreen) { inclusive = true }
                }
            }

            else -> {
                if (!authState.isAuthenticated) {
                    intent?.let {
                        if (!it.hasExtra("isChatNotification") && !it.hasExtra("isIncomingCall")) {
                            navController.navigate(Screen.LoginScreen) {
                                popUpTo(Screen.SplashScreen) { inclusive = true }
                            }
                        }
                    } ?: navController.navigate(Screen.LoginScreen) {
                        popUpTo(Screen.SplashScreen) { inclusive = true }
                    }
                }
            }
        }
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Logo",
            modifier = Modifier.scale(scale.value)
        )

        // Show loading indicator at bottom if loading user
        if (userState.currentUserState is Response.Loading ||
            authState.getCurrentUserIdState is Response.Loading
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }
    }
}