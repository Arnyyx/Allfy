package com.arny.allfy.domain.repository

import android.net.Uri
import com.arny.allfy.domain.model.User
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserDetails(userID: String): Flow<Response<User>>
    suspend fun setUserDetails(user: User, imageUri: Uri?): Flow<Response<Boolean>>
    fun getUsersByIDs(userIDs: List<String>): Flow<Response<List<User>>>

    suspend fun followUser(currentUserId: String, targetUserId: String): Flow<Response<Boolean>>
    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Flow<Response<Boolean>>

    suspend fun getFollowers(followerIds: List<String>): Flow<Response<List<User>>>
    suspend fun getFollowers(userId: String): Flow<Response<List<User>>>
    suspend fun getFollowings(userId: String): Flow<Response<List<User>>>

    suspend fun getFollowingCount(userId: String): Flow<Response<Int>>
    suspend fun getFollowersCount(userId: String): Flow<Response<Int>>
    suspend fun getPostIds(userId: String): Flow<Response<List<String>>>
    suspend fun checkIfFollowing(
        currentUserId: String,
        targetUserId: String
    ): Flow<Response<Boolean>>
}