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
import androidx.navigation.NavHostController
import com.arny.allfy.R
import com.arny.allfy.data.remote.GoogleAuthClient
import com.arny.allfy.presentation.viewmodel.AuthState
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.utils.AutofillTextField
import com.arny.allfy.utils.Screens
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    googleAuthClient: GoogleAuthClient
) {
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                val signInResult = googleAuthClient.getSignInResultFromIntent(
                    result.data ?: return@launch
                )
                signInResult.data?.let { userData ->
                    try {
                        val signInCredential = Identity.getSignInClient(context)
                            .getSignInCredentialFromIntent(result.data)
                        val idToken = signInCredential.googleIdToken
                        if (idToken != null) {
                            authViewModel.signInWithGoogle(idToken)
                        } else {
                            Toast.makeText(
                                context,
                                "Google Sign In failed: No ID token",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            e.message ?: "Google Sign In failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    when (authState.value) {
        is AuthState.Authenticated -> {
            navController.navigate(Screens.FeedScreen.route) {
                popUpTo(Screens.LoginScreen.route) { inclusive = true }
            }
        }

        is AuthState.Error -> Toast.makeText(
            context,
            (authState.value as AuthState.Error).message,
            Toast.LENGTH_SHORT
        ).show()

        else -> {}
    }

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
            val emailState = remember { mutableStateOf("") }
            val passwordState = remember { mutableStateOf("") }

            Spacer(modifier = Modifier.height(48.dp))

            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = "Allfy Logo",
                modifier = Modifier.size(128.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            AutofillTextField(
                value = emailState.value,
                onValueChange = { emailState.value = it },
                label = { Text("Phone number, username, or email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3897F0),
                    unfocusedBorderColor = Color.LightGray
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                autofillTypes = listOf(AutofillType.EmailAddress)
            )

            Spacer(modifier = Modifier.height(16.dp))

            AutofillTextField(
                value = passwordState.value,
                onValueChange = { passwordState.value = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF3897F0),
                    unfocusedBorderColor = Color.LightGray
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                visualTransformation = PasswordVisualTransformation(),
                autofillTypes = listOf(AutofillType.Password)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "By continuing, you agree to Allfy's Terms of Service and Privacy Policy.",
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (authState.value) {
                is AuthState.Loading -> CircularProgressIndicator()
                else -> {
                    Button(
                        onClick = {
                            authViewModel.signInWithEmail(emailState.value, passwordState.value)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3897F0)),
                    ) {
                        Text("Log In", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color.LightGray
                )
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.Gray
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


            OutlinedButton(
                onClick = {
                    scope.launch {
                        val signInIntentSender = googleAuthClient.signIn()
                        launcher.launch(
                            IntentSenderRequest.Builder(
                                signInIntentSender ?: return@launch
                            ).build()
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors()
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
                    Text("Continue with Google")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = {
                //TODO: Handle forgot password
            }) {
                Text("Forgot password?", color = Color(0xFF3897F0))
            }
        }

        Text(
            text = "Don't have an account? Sign up",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .clickable {
                    navController.navigate(Screens.SignUpScreen.route)
                },
            color = Color(0xFF3897F0)
        )
    }
}