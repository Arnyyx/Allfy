package com.arny.allfy.domain.usecase.authentication

import com.arny.allfy.domain.repository.AuthenticationRepository
import javax.inject.Inject

class SignUp @Inject constructor(
    private val repository: AuthenticationRepository

) {
    operator fun invoke(userName: String, email: String, password: String) =
        repository.signUp(userName, email, password)
}