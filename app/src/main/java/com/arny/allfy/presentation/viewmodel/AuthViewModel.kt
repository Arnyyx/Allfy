package com.arny.allfy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.usecase.authentication.AuthenticationUseCases
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
        if (authUseCases.isAuthenticated()) {
            _authState.update { it.copy(isAuthenticated = true) }
        } else {
            _authState.update { it.copy(isAuthenticated = false) }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            authUseCases.signInWithEmail(email, password).collect { response ->
                when (response) {
                    is Response.Loading -> _authState.update { it.copy(isLoading = true) }
                    is Response.Success -> _authState.update { it.copy(isAuthenticated = true) }
                    is Response.Error -> _authState.update { it.copy(errorMessage = response.message) }
                }
            }

        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            authUseCases.signInWithGoogle(idToken).collect { response ->
                when (response) {
                    is Response.Loading -> _authState.update { it.copy(isLoading = true) }
                    is Response.Success -> _authState.update {
                        it.copy(isAuthenticated = true)
                    }

                    is Response.Error -> _authState.update { it.copy(errorMessage = response.message) }
                }
            }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        viewModelScope.launch {
            authUseCases.signUp(username, email, password).collect { response ->
                when (response) {
                    is Response.Loading -> _authState.update { it.copy(isLoading = true) }
                    is Response.Success -> {
                        signInWithEmail(email, password)
                    }

                    is Response.Error -> _authState.update { it.copy(errorMessage = response.message) }
                }

            }
        }
    }


    fun signOut() {
        viewModelScope.launch {
            authUseCases.signOut().collect { response ->
                when (response) {
                    is Response.Loading -> _authState.update { it.copy(isLoading = true) }
                    is Response.Success -> {
                        _authState.update { it.copy(isAuthenticated = false) }
                    }

                    is Response.Error -> _authState.update { it.copy(errorMessage = response.message) }
                }
            }
        }
    }

    fun getCurrentUserId() {
        viewModelScope.launch {
            authUseCases.getCurrentUserId().collect { response ->
                when (response) {
                    is Response.Loading -> _authState.update { it.copy(isLoading = true) }
                    is Response.Error -> _authState.update { it.copy(errorMessage = response.message) }
                    is Response.Success -> _authState.update {
                        it.copy(currentUserId = response.data)
                    }

                }

            }
        }
    }


    fun clearAuthState() {
        viewModelScope.launch {
            _authState.value = AuthState()
        }
    }
}

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null,

    val currentUserId: String = ""
)

