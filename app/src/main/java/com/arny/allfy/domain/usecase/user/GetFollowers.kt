package com.arny.allfy.domain.usecase.user

import com.arny.allfy.domain.repository.UserRepository
import javax.inject.Inject

class GetFollowers @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: String) = repository.getFollowers(userId)
}