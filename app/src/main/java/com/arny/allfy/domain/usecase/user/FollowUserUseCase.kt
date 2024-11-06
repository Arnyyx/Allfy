package com.arny.allfy.domain.usecase.user

import com.arny.allfy.domain.repository.UserRepository
import javax.inject.Inject

class FollowUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(currentUserId: String, targetUserId: String) =
        repository.followUser(currentUserId, targetUserId)
}