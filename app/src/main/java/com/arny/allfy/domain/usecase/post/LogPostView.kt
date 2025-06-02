package com.arny.allfy.domain.usecase.post

import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Response
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class LogPostView @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(userID: String, postID: String): Flow<Response<Boolean>> {
        return repository.logPostView(userID, postID)
    }
}