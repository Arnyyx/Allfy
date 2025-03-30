package com.arny.allfy.domain.repository

import android.net.Uri
import com.arny.allfy.domain.model.User
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserDetails(userID: String): Flow<Response<User>>
    fun setUserDetails(user: User, imageUri: Uri?): Flow<Response<Boolean>>
    fun getUsersByIDs(userIDs: List<String>): Flow<Response<List<User>>>

    fun followUser(currentUserId: String, targetUserId: String): Flow<Response<Boolean>>
    fun unfollowUser(currentUserId: String, targetUserId: String): Flow<Response<Boolean>>

    fun getFollowers(followerIds: List<String>): Flow<Response<List<User>>>
    fun getFollowers(userId: String): Flow<Response<List<User>>>
    fun getFollowingCount(userId: String): Flow<Response<Int>>
    fun getFollowersCount(userId: String): Flow<Response<Int>>
    fun getPostIds(userId: String): Flow<Response<List<String>>>
    fun checkIfFollowing(currentUserId: String, targetUserId: String): Flow<Response<Boolean>>
}