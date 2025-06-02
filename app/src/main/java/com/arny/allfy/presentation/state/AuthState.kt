package com.arny.allfy.presentation.state

import com.arny.allfy.utils.Response

data class AuthState(
    val isAuthenticated: Boolean = false,
    val currentUserId: String = "",

    // Auth actions states
    val signInEmailState: Response<Boolean> = Response.Idle,
    val signInGoogleState: Response<Boolean> = Response.Idle,
    val signUpState: Response<Boolean> = Response.Idle,
    val signOutState: Response<Boolean> = Response.Idle,
    val getCurrentUserIdState: Response<String> = Response.Idle
)