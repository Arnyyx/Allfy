package com.arny.allfy.domain.usecase.Authentication

import com.arny.allfy.domain.repository.AuthenticationRepository
import javax.inject.Inject

class FirebaseSignUp @Inject constructor(
    private val repository: AuthenticationRepository

) {
    operator fun invoke(userName: String, email: String, password: String) =
        repository.firebaseSignUp(userName, email, password)

}