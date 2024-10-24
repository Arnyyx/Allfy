package com.arny.allfy.data.remote

import android.net.Uri
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.repository.UserRepository
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRepository {

    override fun getUserByID(userID: String): Flow<Response<User>> = callbackFlow {
        Response.Loading
        val snapshotListener = firestore.collection(Constants.COLLECTION_NAME_USERS).document(userID)
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

    override fun setUserDetails(user: User, imageUri: Uri?): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            if (imageUri != null) {
                val uploadTask =
                    storage.getReference(Constants.COLLECTION_NAME_USERS + "/" + user.userID)
                        .putFile(imageUri).await()
                user.imageUrl = uploadTask.storage.downloadUrl.await().toString()
            }
            firestore.collection(Constants.COLLECTION_NAME_USERS).document(user.userID)
                .set(user).await()

            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }
}