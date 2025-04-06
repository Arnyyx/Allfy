package com.arny.allfy.domain.usecase.post

import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeletePost @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(postID: String, currentUserID: String): Flow<Response<Boolean>> {
        return repository.deletePost(postID, currentUserID)
    }
}