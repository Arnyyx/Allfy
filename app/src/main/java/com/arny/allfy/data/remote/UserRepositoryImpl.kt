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
import okhttp3.internal.wait
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : UserRepository {

    private suspend fun checkUserHasActiveStory(userId: String): Boolean {
        return try {
            val storyRefs = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(userId)
                .collection("stories")
                .get()
                .await()

            val storyIds = storyRefs.documents.mapNotNull { it.getString("storyID") }

            if (storyIds.isEmpty()) {
                Log.d("UserRepositoryImpl", "No story IDs found for user $userId")
                return false
            }

            val stories = firestore.collection(Constants.COLLECTION_NAME_STORIES)
                .whereIn("storyID", storyIds)
                .get()
                .await()

            stories.documents.any { doc ->
                val timestamp = doc.getTimestamp("timestamp")
                val duration = doc.getLong("duration")

                if (timestamp == null || duration == null) {
                    Log.w(
                        "UserRepositoryImpl",
                        "Invalid story data for storyID: ${doc.id}, timestamp: $timestamp, duration: $duration"
                    )
                    false
                } else {
                    val expiryTime = timestamp.toDate().time + (duration * 1000)
                    val isActive = System.currentTimeMillis() <= expiryTime
                    Log.d(
                        "UserRepositoryImpl",
                        "Story ${doc.id} isActive: $isActive, expiryTime: $expiryTime, currentTime: ${System.currentTimeMillis()}"
                    )
                    isActive
                }
            }
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Error checking active stories for user $userId", e)
            false
        }
    }

    override suspend fun setUserDetails(user: User, imageUri: Uri?): Flow<Response<Boolean>> =
        flow {
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

    override suspend fun followUser(
        currentUserId: String,
        targetUserId: String
    ): Flow<Response<Boolean>> =
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

    override suspend fun unfollowUser(
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
        if (userID.isBlank()) {
            emit(Response.Error("Invalid userID"))
            return@flow
        }
        try {
            val snapshot = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(userID)
                .get()
                .await()

            if (snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                if (user != null) {
                    val hasStory = checkUserHasActiveStory(userID)
                    val userWithStory = user.copy(hasStory = hasStory)
                    emit(Response.Success(userWithStory))
                } else {
                    emit(Response.Error("User data is null"))
                }
            } else {
                emit(Response.Error("User not found"))
            }
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: e.toString()))
        }
    }

    override suspend fun getFollowers(followerIds: List<String>): Flow<Response<List<User>>> =
        flow {
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

                val followers = documents.mapNotNull { doc ->
                    doc.toObject(User::class.java).let { user ->
                        val hasStory = checkUserHasActiveStory(user.userId)
                        user.copy(hasStory = hasStory)
                    }
                }
                emit(Response.Success(followers))
            } catch (e: Exception) {
                emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
            }
        }

    override suspend fun getFollowings(userId: String): Flow<Response<List<User>>> = flow {
        emit(Response.Loading)
        try {
            val followingDocs = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .document(userId)
                .collection("following")
                .get()
                .await()

            val followingIds =
                followingDocs.map { it.getString("userId") ?: "" }.filter { it.isNotEmpty() }
            if (followingIds.isEmpty()) {
                emit(Response.Success(emptyList()))
                return@flow
            }

            val userDocs = firestore.collection(Constants.COLLECTION_NAME_USERS)
                .whereIn("userId", followingIds)
                .get()
                .await()

            val followings = userDocs.mapNotNull { doc ->
                doc.toObject(User::class.java).let { user ->
                    val hasStory = checkUserHasActiveStory(user.userId)
                    user.copy(hasStory = hasStory)
                }
            }
            emit(Response.Success(followings))
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

            val users = documents.mapNotNull { doc ->
                doc.toObject(User::class.java).let { user ->
                    val hasStory = checkUserHasActiveStory(user.userId)
                    user.copy(hasStory = hasStory)
                }
            }
            emit(Response.Success(users))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override suspend fun getFollowers(userId: String): Flow<Response<List<User>>> = flow {
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

            val followers = userDocs.mapNotNull { doc ->
                doc.toObject(User::class.java).let { user ->
                    val hasStory = checkUserHasActiveStory(user.userId)
                    user.copy(hasStory = hasStory)
                }
            }
            emit(Response.Success(followers))
        } catch (e: Exception) {
            emit(Response.Error(e.localizedMessage ?: "An Unexpected Error"))
        }
    }

    override suspend fun getFollowingCount(userId: String): Flow<Response<Int>> = flow {
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

    override suspend fun getFollowersCount(userId: String): Flow<Response<Int>> = flow {
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

    override suspend fun getPostIds(userId: String): Flow<Response<List<String>>> = flow {
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

    override suspend fun checkIfFollowing(
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