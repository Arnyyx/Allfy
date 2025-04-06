package com.arny.allfy.domain.usecase.user

import com.arny.allfy.domain.repository.UserRepository
import javax.inject.Inject

class UnfollowUser @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(currentUserId: String, targetUserId: String) =
        repository.unfollowUser(currentUserId, targetUserId)
}