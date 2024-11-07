package com.arny.allfy.data.remote

import android.net.Uri
import android.util.Log
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.repository.UserRepository
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import okhttp3.internal.wait
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRepository {


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

    override fun followUser(currentUserId: String, targetUserId: String): Flow<Response<Boolean>> =
        flow {
            emit(Response.Loading)
            try {
                firestore.collection(Constants.COLLECTION_NAME_USERS).document(currentUserId)
                    .update("following", FieldValue.arrayUnion(targetUserId)).await()

                firestore.collection(Constants.COLLECTION_NAME_USERS).document(targetUserId)
                    .update("followers", FieldValue.arrayUnion(currentUserId)).await()
                emit(Response.Success(true))
            } catch (e: Exception) {
                emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
            }
        }

    override fun unfollowUser(
        currentUserId: String,
        targetUserId: String
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            firestore.collection(Constants.COLLECTION_NAME_USERS).document(currentUserId)
                .update("following", FieldValue.arrayRemove(targetUserId)).await()

            firestore.collection(Constants.COLLECTION_NAME_USERS).document(targetUserId)
                .update("followers", FieldValue.arrayRemove(currentUserId)).await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun getUserByID(userID: String): Flow<Response<User>> = flow {
        emit(Response.Loading)
        try {
            val snapshot = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(userID)
                .get()
                .await()

            if (snapshot != null) {
                val user = snapshot.toObject(User::class.java)
                emit(Response.Success(user!!))
            }

        } catch (e: Exception) {
            emit(Response.Error(e.message ?: e.toString()))
        }
    }

    override fun getFollowers(followerId: List<String>): Flow<Response<List<User>>> = flow {
        emit(Response.Loading)
        try {
            val documents = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .whereIn(FieldPath.documentId(), followerId)
                .get()
                .await()

            val followers = documents.mapNotNull { it.toObject(User::class.java) }
            emit(Response.Success(followers))
        } catch (exception: Exception) {
            emit(Response.Error(exception.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun getUsersByIDs(userIDs: List<String>): Flow<Response<List<User>>> = flow {
        emit(Response.Loading)
        try {
            val documents = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .whereIn(FieldPath.documentId(), userIDs)
                .get()
                .await()

            val users = documents.mapNotNull { it.toObject(User::class.java) }
            emit(Response.Success(users))
        } catch (exception: Exception) {
            emit(Response.Error(exception.localizedMessage ?: "An Unexpected Error"))
        }
    }
}