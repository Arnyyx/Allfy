package com.arny.allfy.domain.usecase.user

import com.arny.allfy.domain.repository.UserRepository
import javax.inject.Inject

class GetUsersByIDs @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userIDs: List<String>) = repository.getUsersByIDs(userIDs)
}