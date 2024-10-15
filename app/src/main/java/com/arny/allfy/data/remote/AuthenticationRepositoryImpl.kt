package com.arny.allfy.data.remote

import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.repository.AuthenticationRepository
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthenticationRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthenticationRepository {

    private var operationSuccessful = false
    override fun isUserAuthenticatedInFirebase(): Boolean {
        return auth.currentUser != null
    }

    override fun getFirebaseAuthState(): Flow<Boolean> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener {
            trySend(auth.currentUser == null)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    override fun firebaseSignIn(email: String, password: String): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun firebaseSignOut(): Flow<Response<Boolean>> = flow {
        try {
            emit(Response.Loading)
            auth.signOut()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun firebaseSignUp(
        userName: String,
        email: String,
        password: String
    ): Flow<Response<Boolean>> = flow {
        try {
            emit(Response.Loading)

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userID = authResult.user?.uid ?: throw Exception("Failed to get user ID")

            val user =
                User(userName = userName, email = email, password = password, userID = userID)

            firestore.collection(Constants.COLLECTION_NAME_USERS).document(userID).set(user).await()

            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }
}