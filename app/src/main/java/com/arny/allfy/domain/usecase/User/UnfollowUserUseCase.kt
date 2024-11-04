package com.arny.allfy.domain.usecase.User

import com.arny.allfy.domain.repository.UserRepository
import javax.inject.Inject

class UnfollowUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(currentUserId: String, targetUserId: String) =
        repository.unfollowUser(currentUserId, targetUserId)
}