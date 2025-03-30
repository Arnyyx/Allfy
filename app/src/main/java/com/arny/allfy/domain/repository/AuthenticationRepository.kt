package com.arny.allfy.domain.repository

import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {
    fun getCurrentUserId(): Flow<Response<String>>
    fun isAuthenticated(): Boolean
    fun signOut(): Flow<Response<Boolean>>
    fun signInWithEmail(email: String, password: String): Flow<Response<Boolean>>
    fun signUp(userName: String, email: String, password: String): Flow<Response<Boolean>>
    fun signInWithGoogle(idToken: String): Flow<Response<Boolean>>
}