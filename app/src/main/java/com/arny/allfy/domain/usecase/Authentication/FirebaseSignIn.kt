package com.arny.allfy.domain.usecase.Authentication

import com.arny.allfy.domain.repository.AuthenticationRepository
import javax.inject.Inject

class FirebaseSignIn @Inject constructor(
    private val repository: AuthenticationRepository

) {
    operator fun invoke(email: String, password: String) =
        repository.firebaseSignIn(email, password)
}