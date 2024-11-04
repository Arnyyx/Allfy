package com.arny.allfy.domain.usecase.User

import com.arny.allfy.domain.repository.UserRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FollowUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(currentUserId: String, targetUserId: String) =
        repository.followUser(currentUserId, targetUserId)
}