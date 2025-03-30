package com.arny.allfy.domain.usecase.authentication

import com.arny.allfy.domain.repository.AuthenticationRepository
import javax.inject.Inject

class SignOut @Inject constructor(
    private val repository: AuthenticationRepository

) {
    operator fun invoke() = repository.signOut()
}