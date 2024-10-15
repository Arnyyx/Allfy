package com.arny.allfy.data.remote

import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.repository.UserRepository
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : UserRepository {
    override fun getUserDetails(userID: String): Flow<Response<User>> = callbackFlow {
        Response.Loading
        val snapshotListener = firestore.collection("users").document(userID)
            .addSnapshotListener { snapshot, e ->
                val response = if (snapshot != null) {
                    val user = snapshot.toObject(User::class.java)
                    Response.Success(user!!)
                } else {
                    Response.Error(e?.message ?: e.toString())
                }
                trySend(response).isSuccess
            }
        awaitClose {
            snapshotListener.remove()
        }
    }

    override fun setUserDetails(
        userID: String,
        name: String,
        userName: String,
        bio: String
    ): Flow<Response<Boolean>> = flow {
        try {
            val user = mutableMapOf<String, String>()
            user["name"] = name
            user["userName"] = userName
            user["bio"] = bio
            firestore.collection(Constants.COLLECTION_NAME_USERS).document(userID)
                .update(user as Map<String, Any>).await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }

    }
}