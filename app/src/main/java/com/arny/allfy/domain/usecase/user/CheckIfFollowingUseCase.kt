package com.arny.allfy.domain.usecase.user

import com.arny.allfy.domain.repository.UserRepository
import javax.inject.Inject

class CheckIfFollowingUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(currentUserId: String, targetUserId: String) =
        userRepository.checkIfFollowing(currentUserId, targetUserId)
}
