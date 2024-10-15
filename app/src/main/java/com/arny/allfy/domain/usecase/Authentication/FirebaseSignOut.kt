package com.arny.allfy.domain.usecase.Authentication

import com.arny.allfy.domain.repository.AuthenticationRepository
import javax.inject.Inject

class FirebaseSignOut @Inject constructor(
    private val repository: AuthenticationRepository

) {
    operator fun invoke() = repository.firebaseSignOut()
}