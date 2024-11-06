package com.arny.allfy.domain.usecase.authentication

import com.arny.allfy.domain.repository.AuthenticationRepository
import javax.inject.Inject

class FirebaseAuthState @Inject constructor(
    private val repository: AuthenticationRepository
) {
    operator fun invoke() = repository.getFirebaseAuthState()

}