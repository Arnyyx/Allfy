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
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arny.allfy.R
import com.arny.allfy.data.remote.GoogleAuthClient
import com.arny.allfy.presentation.viewmodel.AuthState
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    googleAuthClient: GoogleAuthClient = hiltViewModel<AuthViewModel>().googleAuthClient
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authState = authViewModel.authState.observeAsState()
    val signUpState = authViewModel.signUpState.observeAsState()

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
                SignUpForm(
                    authViewModel = authViewModel,
                    authState = authState.value,
                    signUpState = signUpState.value,
                    googleAuthClient = googleAuthClient,
                    scope = scope,
                    context = context,
                    navController = navController
                )
                LoginPrompt(navController)
            }
        }

    }
}

@Composable
private fun SignUpForm(
    authViewModel: AuthViewModel,
    authState: AuthState?,
    signUpState: Response<*>?,
    googleAuthClient: GoogleAuthClient,
    scope: CoroutineScope,
    context: Any,
    navController: NavHostController
) {
    val nameState = remember { mutableStateOf("") }
    val usernameState = remember { mutableStateOf("") }
    val emailState = remember { mutableStateOf("") }
    val passwordState = remember { mutableStateOf("") }

    // Xử lý trạng thái đăng ký và đăng nhập
    LaunchedEffect(signUpState) {
        when (signUpState) {
            is Response.Success<*> -> {
                if (signUpState.data as Boolean) {
                    navController.navigate(Screens.ProfileScreen.route) {
                        popUpTo(Screens.SignUpScreen.route) { inclusive = true }
                    }
                }
            }

            is Response.Error -> {
                Toast.makeText(context as Activity, signUpState.message, Toast.LENGTH_SHORT).show()
            }

            else -> {}
        }
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate(Screens.ProfileScreen.route) {
                    popUpTo(Screens.SignUpScreen.route) { inclusive = true }
                }
            }

            is AuthState.Error -> {
                Toast.makeText(context as Activity, authState.message, Toast.LENGTH_SHORT).show()
            }

            else -> {}
        }
    }

    Spacer(modifier = Modifier.height(48.dp))

    Image(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = "Allfy Logo",
        modifier = Modifier.size(128.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = nameState.value,
        onValueChange = { nameState.value = it },
        label = { Text("Full Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        ),
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
        value = usernameState.value,
        onValueChange = { usernameState.value = it },
        label = { Text("Username") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        ),
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
        value = emailState.value,
        onValueChange = { emailState.value = it },
        label = { Text("Email") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
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
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
        ),
        shape = RoundedCornerShape(8.dp)
    )

    Spacer(modifier = Modifier.height(24.dp))

    val isLoading = signUpState is Response.Loading || authState is AuthState.Loading
    Button(
        onClick = {
            authViewModel.signUp(
                usernameState.value,
                nameState.value,
                emailState.value,
                passwordState.value
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3897F0)),
        shape = RoundedCornerShape(8.dp),
        enabled = !isLoading && nameState.value.isNotBlank() && usernameState.value.isNotBlank() &&
                emailState.value.isNotBlank() && passwordState.value.isNotBlank()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text("Sign Up", color = Color.White, fontWeight = FontWeight.Bold)
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
                } ?: run {
                    Toast.makeText(
                        context,
                        result.errorMessage ?: "Google Sign Up failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        enabled = !isLoading
    ) {
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
            Text("Sign up with Google", color = MaterialTheme.colorScheme.onSurface)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "By signing up, you agree to our Terms, Data Policy and Cookies Policy.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}

@Composable
private fun DividerWithText(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun LoginPrompt(navController: NavHostController) {
    Text(
        text = "Have an account? Log in",
        modifier = Modifier
            .padding(bottom = 16.dp)
            .clickable {
                navController.popBackStack()
            },
        color = Color(0xFF3897F0),
        style = MaterialTheme.typography.bodyMedium
    )
}