package com.arny.allfy.presentation.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arny.allfy.R
import com.arny.allfy.presentation.common.Toast
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.ui.theme.Typography
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screens

@Composable
fun SignUpScreen(navController: NavHostController, authViewModel: AuthViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val userNameState = remember { mutableStateOf("") }
            val emailState = remember { mutableStateOf("") }
            val passwordState = remember { mutableStateOf("") }
            Image(
                painterResource(R.drawable.ic_logo),
                "SignUpScreen Logo",
                modifier = Modifier
                    .width(200.dp)
            )
            Text(
                text = "SIGN UP",
                modifier = Modifier.padding(10.dp),
                style = Typography.headlineMedium,
            )
            OutlinedTextField(
                value = userNameState.value, onValueChange = { userNameState.value = it },
                label = { Text("Enter your user name") },
            )
            OutlinedTextField(
                value = emailState.value, onValueChange = { emailState.value = it },
                label = { Text("Enter your e-mail") }, modifier = Modifier.padding(10.dp),
            )
            OutlinedTextField(
                value = passwordState.value, onValueChange = { passwordState.value = it },
                label = { Text("Enter your password") },
                visualTransformation = PasswordVisualTransformation(),
            )
            Button(
                onClick = {
                    authViewModel.signUp(userNameState.value, emailState.value, passwordState.value)
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Sign Up")
                when (val response = authViewModel.signUpState.value) {
                    is Response.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is Response.Success -> {
                        if (response.data) {
                            LaunchedEffect(key1 = true) {
                                navController.navigate(Screens.ProfileScreen.route) {
                                    popUpTo(Screens.SignUpScreen.route) {
                                        inclusive = true
                                    }
                                }
                            }
                        } else {
                            Toast("Sign Up failed")
                        }
                    }

                    is Response.Error -> {
                        Log.d("TAG", "SignUpScreen: ${response.message}")
                        Toast(response.message)
                    }
                }
            }
            Text("Already have an account? Sign in", modifier = Modifier.clickable {
                navController.navigate(Screens.LoginScreen.route) {
                    launchSingleTop = true
                }
            })
        }
    }
}