package com.arny.allfy.domain.usecase.authentication

import com.arny.allfy.domain.repository.AuthenticationRepository
import javax.inject.Inject

class GetCurrentUserId @Inject constructor(
    private val repository: AuthenticationRepository
) {
    operator fun invoke() = repository.getCurrentUserId()
}