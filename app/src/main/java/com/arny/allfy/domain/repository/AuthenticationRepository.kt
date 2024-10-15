package com.arny.allfy.domain.repository

import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {
    fun isUserAuthenticatedInFirebase(): Boolean
    fun getFirebaseAuthState(): Flow<Boolean>
    fun firebaseSignIn(email: String, password: String): Flow<Response<Boolean>>
    fun firebaseSignOut(): Flow<Response<Boolean>>
    fun firebaseSignUp(userName: String, email: String, password: String): Flow<Response<Boolean>>
}