package com.arny.allfy.presentation.ui

import android.app.Activity
import android.util.Log
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import com.arny.allfy.R
import com.arny.allfy.presentation.viewmodel.AuthState
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Screen
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel
) {
    val scale = remember { Animatable(0f) }
    val authState = authViewModel.authState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val intent = activity?.intent

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 0.5f,
            animationSpec = tween(
                durationMillis = 500,
                easing = { it * it }
            )
        )
    }

    LaunchedEffect(authState.value.currentUserId, intent) {
        if (authState.value.currentUserId.isNotEmpty()) {
            userViewModel.getCurrentUser(authState.value.currentUserId)

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
    }

    LaunchedEffect(authState.value) {
        when {
            authState.value.isLoading -> {}

            authState.value.isAuthenticated -> {
                authViewModel.getCurrentUserId()
            }

            else -> {
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

        authState.value.errorMessage?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "Logo",
            modifier = Modifier.scale(scale.value)
        )
    }
}