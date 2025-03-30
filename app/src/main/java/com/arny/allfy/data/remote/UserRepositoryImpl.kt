package com.arny.allfy.data.remote

import android.net.Uri
import android.util.Log
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.repository.UserRepository
import com.arny.allfy.utils.Constants
import com.arny.allfy.utils.Response
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRepository {

    override fun setUserDetails(user: User, imageUri: Uri?): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val updates = mutableMapOf<String, Any>()
            if (imageUri != null) {
                val storageRef =
                    storage.getReference("${Constants.COLLECTION_NAME_USERS}/${user.userId}/profile.jpg")
                storageRef.putFile(imageUri).await()
                val imageUrl = storageRef.downloadUrl.await().toString()
                updates["imageUrl"] = imageUrl
            }
            if (user.username.isNotEmpty()) updates["username"] = user.username
            if (user.name.isNotEmpty()) updates["name"] = user.name
            if (user.bio?.isNotEmpty()!!) updates["bio"] = user.bio
            if (user.email.isNotEmpty()) updates["email"] = user.email

            firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(user.userId)
                .update(updates)
                .await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun followUser(currentUserId: String, targetUserId: String): Flow<Response<Boolean>> =
        flow {
            emit(Response.Loading)
            try {
                val followData = mapOf(
                    "userId" to targetUserId,
                    "timestamp" to System.currentTimeMillis()
                )
                val followerData = mapOf(
                    "userId" to currentUserId,
                    "timestamp" to System.currentTimeMillis()
                )

                firestore.collection(Constants.COLLECTION_NAME_USERS)
                    .document(currentUserId)
                    .collection("following")
                    .document(targetUserId)
                    .set(followData)
                    .await()

                firestore.collection(Constants.COLLECTION_NAME_USERS)
                    .document(targetUserId)
                    .collection("followers")
                    .document(currentUserId)
                    .set(followerData)
                    .await()

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
            firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(currentUserId)
                .collection("following")
                .document(targetUserId)
                .delete()
                .await()

            firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(targetUserId)
                .collection("followers")
                .document(currentUserId)
                .delete()
                .await()

            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun getUserDetails(userID: String): Flow<Response<User>> = flow {
        emit(Response.Loading)
        try {
            val snapshot = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(userID)
                .get()
                .await()

            if (snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                emit(Response.Success(user!!))
            } else {
                emit(Response.Error("User not found"))
            }
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: e.toString()))
        }
    }

    override fun getFollowers(followerIds: List<String>): Flow<Response<List<User>>> = flow {
        emit(Response.Loading)
        try {
            if (followerIds.isEmpty()) {
                emit(Response.Success(emptyList()))
                return@flow
            }
            val documents = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .whereIn("userId", followerIds)
                .get()
                .await()

            val followers = documents.mapNotNull { it.toObject(User::class.java) }
            emit(Response.Success(followers))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun getUsersByIDs(userIDs: List<String>): Flow<Response<List<User>>> = flow {
        emit(Response.Loading)
        try {
            if (userIDs.isEmpty()) {
                emit(Response.Success(emptyList()))
                return@flow
            }
            val documents = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .whereIn("userId", userIDs)
                .get()
                .await()

            val users = documents.mapNotNull { it.toObject(User::class.java) }
            emit(Response.Success(users))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun getFollowers(userId: String): Flow<Response<List<User>>> = flow {
        emit(Response.Loading)
        try {
            val followerDocs = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(userId)
                .collection("followers")
                .get()
                .await()

            val followerIds =
                followerDocs.map { it.getString("userId") ?: "" }.filter { it.isNotEmpty() }
            if (followerIds.isEmpty()) {
                emit(Response.Success(emptyList()))
                return@flow
            }

            val userDocs = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .whereIn("userId", followerIds)
                .get()
                .await()

            val followers = userDocs.mapNotNull { it.toObject(User::class.java) }
            emit(Response.Success(followers))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun getFollowingCount(userId: String): Flow<Response<Int>> = flow {
        emit(Response.Loading)
        try {
            val snapshot = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(userId)
                .collection("following")
                .get()
                .await()
            emit(Response.Success(snapshot.size()))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun getFollowersCount(userId: String): Flow<Response<Int>> = flow {
        emit(Response.Loading)
        try {
            val snapshot = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(userId)
                .collection("followers")
                .get()
                .await()
            emit(Response.Success(snapshot.size()))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun getPostIds(userId: String): Flow<Response<List<String>>> = flow {
        emit(Response.Loading)
        try {
            val snapshot = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(userId)
                .collection("posts")
                .get()
                .await()
            val postIds = snapshot.documents.mapNotNull { it.getString("postId") }
            emit(Response.Success(postIds))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override fun checkIfFollowing(
        currentUserId: String,
        targetUserId: String
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val snapshot = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(currentUserId)
                .collection("following")
                .document(targetUserId)
                .get()
                .await()
            emit(Response.Success(snapshot.exists()))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }
}