package com.arny.allfy.domain.usecase.user

import com.arny.allfy.domain.repository.UserRepository
import javax.inject.Inject

class CheckIfFollowing @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(currentUserId: String, targetUserId: String) =
        userRepository.checkIfFollowing(currentUserId, targetUserId)
}
