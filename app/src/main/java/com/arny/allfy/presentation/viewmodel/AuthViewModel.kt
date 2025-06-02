package com.arny.allfy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.usecase.authentication.AuthenticationUseCases
import com.arny.allfy.presentation.state.AuthState
import com.arny.allfy.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCases: AuthenticationUseCases,
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        val isAuth = authUseCases.isAuthenticated()
        _authState.update { it.copy(isAuthenticated = isAuth) }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            authUseCases.signInWithEmail(email, password).collect { response ->
                _authState.update { it.copy(signInEmailState = response) }

                if (response is Response.Success) {
                    _authState.update { it.copy(isAuthenticated = true) }
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            authUseCases.signInWithGoogle(idToken).collect { response ->
                _authState.update { it.copy(signInGoogleState = response) }

                if (response is Response.Success) {
                    _authState.update { it.copy(isAuthenticated = true) }
                }
            }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        viewModelScope.launch {
            authUseCases.signUp(username, email, password).collect { response ->
                _authState.update { it.copy(signUpState = response) }

                if (response is Response.Success) {
                    signInWithEmail(email, password)
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authUseCases.signOut().collect { response ->
                _authState.update { it.copy(signOutState = response) }

                if (response is Response.Success) {
                    _authState.update {
                        it.copy(
                            isAuthenticated = false,
                            currentUserId = ""
                        )
                    }
                }
            }
        }
    }

    fun getCurrentUserId() {
        viewModelScope.launch {
            authUseCases.getCurrentUserId().collect { response ->
                _authState.update { it.copy(getCurrentUserIdState = response) }

                if (response is Response.Success) {
                    _authState.update { it.copy(currentUserId = response.data) }
                }
            }
        }
    }

    // Reset Functions
    fun resetSignInEmailState() {
        _authState.update { it.copy(signInEmailState = Response.Idle) }
    }

    fun resetSignInGoogleState() {
        _authState.update { it.copy(signInGoogleState = Response.Idle) }
    }

    fun resetSignUpState() {
        _authState.update { it.copy(signUpState = Response.Idle) }
    }

    fun resetSignOutState() {
        _authState.update { it.copy(signOutState = Response.Idle) }
    }

    fun resetGetCurrentUserIdState() {
        _authState.update { it.copy(getCurrentUserIdState = Response.Idle) }
    }

    fun clearAuthState() {
        _authState.value = AuthState()
    }
}
