package com.arny.allfy.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.data.remote.GoogleAuthClient
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.usecase.authentication.AuthenticationUseCases
import com.arny.allfy.domain.usecase.user.SetUserDetailsUseCase
import com.arny.allfy.utils.Response
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthenticationUseCases,
    private val setUserDetailsUseCase: SetUserDetailsUseCase,
    val googleAuthClient: GoogleAuthClient
) : ViewModel() {
    private val _signUpState = MutableLiveData<Response<Boolean>>()
    val signUpState: LiveData<Response<Boolean>> = _signUpState
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    init {
        checkAuthStatus()
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            authUseCases.firebaseSignIn(email, password).collect {
                _authState.value = it
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            authUseCases.signInWithGoogle(idToken).collect {
                _authState.value = it
            }
        }
    }

    fun signUp(username: String, name: String, email: String, password: String) {
        viewModelScope.launch {
            _signUpState.value = Response.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    val userData = User(
                        userId = user.uid,
                        username = username,
                        name = name,
                        email = email,
                        imageUrl = "",
                        bio = "",
                        fcmToken = ""
                    )
                    setUserDetailsUseCase(userData, null).collect { response ->
                        when (response) {
                            is Response.Success -> {
                                val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build()
                                user.updateProfile(profileUpdates).await()
                                _signUpState.value = Response.Success(true)
                            }

                            is Response.Error -> _signUpState.value =
                                Response.Error(response.message)

                            is Response.Loading -> _signUpState.value = Response.Loading
                        }
                    }
                }
            } catch (e: Exception) {
                _signUpState.value = Response.Error(e.message ?: "Sign up failed")
            }
        }
    }


    private fun checkAuthStatus() {
        if (auth.currentUser == null) {
            _authState.value = AuthState.Unauthenticated
        } else {
            _authState.value = AuthState.Authenticated
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            authUseCases.firebaseSignOut().collect { state ->
                when (state) {
                    is AuthState.Unauthenticated -> {
                        val preferences =
                            context.getSharedPreferences("FCMPrefs", Context.MODE_PRIVATE)
                        preferences.edit().remove("fcmToken").apply()

                        _authState.value = state
                        clear()
                    }

                    is AuthState.Loading -> _authState.value = state
                    is AuthState.Error -> _authState.value = state
                    else -> {}
                }
            }
        }
    }


    fun clear() {
        viewModelScope.launch {
            _signUpState.value = Response.Success(false)
            _authState.value = AuthState.Unauthenticated
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
    object Unauthenticated : AuthState()
}

