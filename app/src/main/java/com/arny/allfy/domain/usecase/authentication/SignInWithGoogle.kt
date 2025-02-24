package com.arny.allfy.domain.usecase.authentication

import com.arny.allfy.domain.repository.AuthenticationRepository
import javax.inject.Inject

class SignInWithGoogle @Inject constructor(
    private val repository: AuthenticationRepository
) {
    operator fun invoke(idToken: String) = repository.signInWithGoogle(idToken)
}