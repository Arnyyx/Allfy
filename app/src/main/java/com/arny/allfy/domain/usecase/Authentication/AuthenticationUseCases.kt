package com.arny.allfy.domain.usecase.Authentication

data class AuthenticationUseCases(
    val isUserAuthenticated: IsUserAuthenticated,
    val firebaseAuthState: FirebaseAuthState,
    val firebaseSignOut: FirebaseSignOut,
    val firebaseSignIn: FirebaseSignIn,
    val firebaseSignUp: FirebaseSignUp
) {
}