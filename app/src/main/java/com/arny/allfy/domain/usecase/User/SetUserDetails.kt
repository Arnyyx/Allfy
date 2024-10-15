package com.arny.allfy.domain.usecase.User

import com.arny.allfy.domain.repository.UserRepository
import javax.inject.Inject

class SetUserDetails @Inject constructor(
    private val repository: UserRepository

) {
    operator fun invoke(
        userID: String,
        name: String,
        userName: String,
        bio: String
    ) = repository.setUserDetails(userID, name, userName, bio)

}