package com.arny.allfy.data.remote

import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.repository.AuthenticationRepository
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthenticationRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthenticationRepository {

    override fun getCurrentUserId(): Flow<Response<String>> = flow {
        emit(Response.Loading)
        try {
            val userId = firebaseAuth.currentUser?.uid ?: throw Exception("User ID is null")
            emit(Response.Success(userId))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun isAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override fun signInWithEmail(email: String, password: String): Flow<Response<Boolean>> =
        flow {
            emit(Response.Loading)
            try {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                emit(Response.Success(true))
            } catch (e: Exception) {
                emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
            }
        }

    override fun signOut(): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            firebaseAuth.signOut()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "Error signing out"))
        }
    }.flowOn(Dispatchers.IO)

    override fun signUp(
        userName: String,
        email: String,
        password: String
    ): Flow<Response<Boolean>> = flow {
        try {
            emit(Response.Loading)
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val userID = authResult.user?.uid ?: throw Exception("Failed to get otherUser ID")
            val user = User(userId = userID, username = userName, email = email)
            firestore.collection(Constants.COLLECTION_NAME_USERS).document(userID).set(user).await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun signInWithGoogle(idToken: String): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false
            val firebaseUser = authResult.user ?: throw Exception("Firebase otherUser is null")

            if (isNewUser) {
                val user = User(
                    userId = firebaseUser.uid,
                    username = firebaseUser.email ?: "",
                    name = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: ""
                )
                firestore.collection(Constants.COLLECTION_NAME_USERS)
                    .document(user.userId)
                    .set(user)
                    .await()
            }
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }
}