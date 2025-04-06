package com.arny.allfy.domain.usecase.user

import com.arny.allfy.domain.repository.UserRepository
import javax.inject.Inject

class GetFollowersCount @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String) = userRepository.getFollowersCount(userId)
}
