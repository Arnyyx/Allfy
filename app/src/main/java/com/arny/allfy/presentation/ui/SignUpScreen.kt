package com.arny.allfy.presentation.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.arny.allfy.R
import com.arny.allfy.data.remote.GoogleAuthClient
import com.arny.allfy.presentation.viewmodel.AuthState
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screens
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignUpScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    googleAuthClient: GoogleAuthClient = hiltViewModel<AuthViewModel>().googleAuthClient
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authState = authViewModel.authState.observeAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val userNameState = remember { mutableStateOf("") }
            val emailState = remember { mutableStateOf("") }
            val passwordState = remember { mutableStateOf("") }

            Spacer(modifier = Modifier.height(48.dp))

            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(128.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = userNameState.value,
                onValueChange = { userNameState.value = it },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
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
                )
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
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    authViewModel.signUp(userNameState.value, emailState.value, passwordState.value)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3897F0))
            ) {
                Text("Sign Up", color = Color.White)
            }

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
                    .height(50.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_google),
                        contentDescription = "Google Sign Up",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign up with Google")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "By signing up, you agree to our Terms, Data Policy and Cookies Policy.",
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            when (val response = authViewModel.signUpState.value) {
                is Response.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }

                is Response.Success -> {
                    if (response.data) {
                        LaunchedEffect(Unit) {
                            navController.navigate(Screens.ProfileScreen.route) {
                                popUpTo(Screens.SignUpScreen.route) { inclusive = true }
                            }
                        }
                    }
                }

                is Response.Error -> {
                    LaunchedEffect(response.message) {
                        Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            when (val state = authState.value) {
                is AuthState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }
                is AuthState.Authenticated -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(Screens.ProfileScreen.route) {
                            popUpTo(Screens.SignUpScreen.route) { inclusive = true }
                        }
                    }
                }
                is AuthState.Error -> {
                    LaunchedEffect(state.message) {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
                else -> {}
            }
        }

        Text(
            text = "Have an account? Log in",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .clickable {
                    navController.popBackStack()
                },
            color = Color(0xFF3897F0)
        )
    }
}