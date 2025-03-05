package com.arny.allfy.domain.usecase.user

import com.arny.allfy.domain.repository.UserRepository
import javax.inject.Inject

class GetFollowersFromSubcollectionUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(userId: String) = repository.getFollowersFromSubcollection(userId)
}