package com.arny.allfy.presentation.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.data.remote.GoogleAuthClient
import com.arny.allfy.domain.usecase.authentication.AuthenticationUseCases
import com.arny.allfy.utils.Response
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthenticationUseCases,
    val googleAuthClient: GoogleAuthClient
) : ViewModel() {
    private val _signUpState = mutableStateOf<Response<Boolean>>(Response.Success(false))
    val signUpState: State<Response<Boolean>> = _signUpState

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

    fun signUp(userName: String, email: String, password: String) {
        viewModelScope.launch {
            authUseCases.firebaseSignUp(userName, email, password).collect {
                _authState.value = it
                if (it is AuthState.Authenticated) {
                    _signUpState.value = Response.Success(true)
                } else if (it is AuthState.Error) {
                    _signUpState.value = Response.Error(it.message)
                }
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

    fun signOut() {
        viewModelScope.launch {
            authUseCases.firebaseSignOut().collect { it ->
                if (it is AuthState.Unauthenticated) {
                    _authState.value = it
                    clear()
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
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}
