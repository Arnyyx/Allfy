package com.arny.allfy.domain.repository

import com.arny.allfy.domain.model.User
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserDetails(userID: String): Flow<Response<User>>
    fun setUserDetails(
        userID: String,
        name: String,
        userName: String,
        bio: String
    ): Flow<Response<Boolean>>
}