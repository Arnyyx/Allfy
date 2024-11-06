package com.arny.allfy.domain.repository

import android.net.Uri
import com.arny.allfy.domain.model.User
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserByID(userID: String): Flow<Response<User>>
    fun setUserDetails(user: User, imageUri: Uri?): Flow<Response<Boolean>>
    fun followUser(currentUserId: String, targetUserId: String): Flow<Response<Boolean>>
    fun unfollowUser(currentUserId: String, targetUserId: String): Flow<Response<Boolean>>
    fun getFollowers(followerId: List<String>): Flow<Response<List<User>>>
}