package com.arny.allfy.domain.usecase.authentication

data class AuthenticationUseCases(
    val isUserAuthenticated: IsUserAuthenticated,
    val firebaseAuthState: FirebaseAuthState,
    val firebaseSignOut: FirebaseSignOut,
    val firebaseSignIn: FirebaseSignIn,
    val firebaseSignUp: FirebaseSignUp,
    val getCurrentUserID: GetCurrentUserID,
    val signInWithGoogle: SignInWithGoogle
)