package com.arny.allfy.domain.usecase.authentication

data class AuthenticationUseCases(
    val getCurrentUserId: GetCurrentUserId,
    val isAuthenticated: IsAuthenticated,
    val signOut: SignOut,
    val signInWithEmail: SignInWithEmail,
    val signUp: SignUp,
    val signInWithGoogle: SignInWithGoogle,
)