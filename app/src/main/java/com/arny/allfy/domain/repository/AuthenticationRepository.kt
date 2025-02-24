package com.arny.allfy.domain.repository

import com.arny.allfy.presentation.viewmodel.AuthState
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {
    fun isUserAuthenticatedInFirebase(): Boolean
    fun getFirebaseAuthState(): Flow<Boolean>
    fun getCurrentUserID(): Flow<Response<String>>
    fun firebaseSignOut(): Flow<AuthState>
    fun firebaseSignIn(email: String, password: String): Flow<AuthState>
    fun firebaseSignUp(userName: String, email: String, password: String): Flow<AuthState>
    fun signInWithGoogle(idToken: String): Flow<AuthState>
}