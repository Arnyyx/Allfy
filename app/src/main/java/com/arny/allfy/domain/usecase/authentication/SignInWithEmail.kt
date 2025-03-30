package com.arny.allfy.domain.usecase.authentication

import com.arny.allfy.domain.repository.AuthenticationRepository
import javax.inject.Inject

class SignInWithEmail @Inject constructor(
    private val repository: AuthenticationRepository

) {
    operator fun invoke(email: String, password: String) =
        repository.signInWithEmail(email, password)
}