package com.arny.allfy.data.remote

import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.repository.AuthenticationRepository
import com.arny.allfy.presentation.viewmodel.AuthState
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthenticationRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthenticationRepository {

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

    override fun getCurrentUserID(): Flow<Response<String>> = flow {
        emit(Response.Loading)
        try {
            val userID = auth.currentUser?.uid ?: throw Exception("User ID not found")
            emit(Response.Success(userID))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun firebaseSignIn(email: String, password: String): Flow<AuthState> = flow {
        emit(AuthState.Loading)
        try {
            auth.signInWithEmailAndPassword(email, password).await()
            emit(AuthState.Authenticated)
        } catch (e: Exception) {
            emit(AuthState.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun firebaseSignOut(): Flow<AuthState> = flow {
        try {
            emit(AuthState.Loading)
            auth.signOut()
            emit(AuthState.Unauthenticated)
        } catch (e: Exception) {
            emit(AuthState.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun firebaseSignUp(
        userName: String,
        email: String,
        password: String
    ): Flow<AuthState> = flow {
        try {
            emit(AuthState.Loading)
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userID = authResult.user?.uid ?: throw Exception("Failed to get user ID")
            val user = User(userID = userID, userName = userName, email = email)
            firestore.collection(Constants.COLLECTION_NAME_USERS).document(userID).set(user).await()
            emit(AuthState.Authenticated)
        } catch (e: Exception) {
            emit(AuthState.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun signInWithGoogle(idToken: String): Flow<AuthState> = flow {
        emit(AuthState.Loading)
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false
            val firebaseUser = authResult.user ?: throw Exception("Firebase user is null")

            if (isNewUser) {
                val user = User(
                    userID = firebaseUser.uid,
                    userName = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: ""
                )
                firestore.collection(Constants.COLLECTION_NAME_USERS)
                    .document(user.userID)
                    .set(user)
                    .await()
            }
            emit(AuthState.Authenticated)
        } catch (e: Exception) {
            emit(AuthState.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }
}
