package com.arny.allfy.presentation.ui

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
import com.arny.allfy.utils.Screen

@Composable
fun SplashScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val scale = remember { Animatable(0f) }
    val authState = authViewModel.authState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 0.5f,
            animationSpec = tween(
                durationMillis = 500,
                easing = { it * it }
            )
        )
    }

    LaunchedEffect(authState.value) {
        when {
            authState.value.isLoading -> {}

            authState.value.isAuthenticated -> {
                navController.navigate(Screen.FeedScreen) {
                    popUpTo(Screen.SplashScreen) { inclusive = true }
                }
            }

            else -> {
                navController.navigate(Screen.LoginScreen) {
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