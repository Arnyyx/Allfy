package com.arny.allfy.domain.usecase.User

import android.net.Uri
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.repository.UserRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SetUserDetails @Inject constructor(
    private val repository: UserRepository

) {
    operator fun invoke(user: User, imageUri: Uri?): Flow<Response<Boolean>> {
        return repository.setUserDetails(user, imageUri)
    }
}