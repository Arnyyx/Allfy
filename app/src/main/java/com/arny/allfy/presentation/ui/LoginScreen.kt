package com.arny.allfy.presentation.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arny.allfy.R
import com.arny.allfy.data.remote.GoogleAuthClient
import com.arny.allfy.presentation.state.AuthState
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.utils.Screen
import com.arny.allfy.utils.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    googleAuthClient: GoogleAuthClient
) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            navController.navigate(Screen.SplashScreen) {
                popUpTo(Screen.LoginScreen) { inclusive = true }
            }
        }
    }

    // Handle sign in errors
    LaunchedEffect(authState.signInEmailState) {
        when (val state = authState.signInEmailState) {
            is Response.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                authViewModel.resetSignInEmailState()
            }

            else -> {}
        }
    }

    LaunchedEffect(authState.signInGoogleState) {
        when (val state = authState.signInGoogleState) {
            is Response.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                authViewModel.resetSignInGoogleState()
            }

            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 2 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoginForm(
                    authState = authState,
                    authViewModel = authViewModel,
                    googleAuthClient = googleAuthClient,
                    scope = scope,
                    context = context
                )
                SignUpPrompt(navController)
            }
        }
    }
}

@Composable
private fun LoginForm(
    authState: AuthState,
    authViewModel: AuthViewModel,
    googleAuthClient: GoogleAuthClient,
    scope: CoroutineScope,
    context: Any
) {
    val emailState = remember { mutableStateOf("") }
    val passwordState = remember { mutableStateOf("") }

    val isLoading = authState.signInEmailState is Response.Loading ||
            authState.signInGoogleState is Response.Loading

    Spacer(modifier = Modifier.height(48.dp))

    Image(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = "Allfy Logo",
        modifier = Modifier.size(128.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = emailState.value,
        onValueChange = { emailState.value = it },
        label = { Text("Phone number, username, or email") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        enabled = !isLoading,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(8.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = passwordState.value,
        onValueChange = { passwordState.value = it },
        label = { Text("Password") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        visualTransformation = PasswordVisualTransformation(),
        enabled = !isLoading,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(8.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "By continuing, you agree to Allfy's Terms of Service and Privacy Policy.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier
    )

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = {
            authViewModel.signInWithEmail(emailState.value, passwordState.value)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3897F0)),
        shape = RoundedCornerShape(8.dp),
        enabled = emailState.value.isNotBlank() && passwordState.value.isNotBlank() && !isLoading
    ) {
        if (isLoading && authState.signInEmailState is Response.Loading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text("Log In", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    DividerWithText("OR")

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
        onClick = {
            scope.launch {
                val result = googleAuthClient.signIn(context as Activity)
                result.googleIdToken?.let { idToken ->
                    authViewModel.signInWithGoogle(idToken)
                } ?: run { }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        enabled = !isLoading
    ) {
        if (isLoading && authState.signInGoogleState is Response.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = "Google Icon",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue with Google", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    TextButton(onClick = { /* TODO: Handle forgot password */ }) {
        Text("Forgot password?", color = Color(0xFF3897F0))
    }
}

@Composable
private fun DividerWithText(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun SignUpPrompt(navController: NavHostController) {
    Text(
        text = "Don't have an account? Sign up",
        modifier = Modifier
            .padding(bottom = 16.dp)
            .clickable {
                navController.navigate(Screen.SignUpScreen)
            },
        color = Color(0xFF3897F0),
        style = MaterialTheme.typography.bodyMedium
    )
}