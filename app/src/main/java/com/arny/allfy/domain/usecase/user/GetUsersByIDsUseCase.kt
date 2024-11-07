package com.arny.allfy.domain.usecase.user

import com.arny.allfy.domain.repository.UserRepository
import javax.inject.Inject

class GetUsersByIDsUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(followerId: List<String>) =
        repository.getUsersByIDs(followerId)
}