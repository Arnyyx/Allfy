package com.arny.allfy.domain.usecase.user

import com.arny.allfy.domain.repository.UserRepository
import javax.inject.Inject

class GetUserDetails @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(userID: String) = repository.getUserByID(userID)
}