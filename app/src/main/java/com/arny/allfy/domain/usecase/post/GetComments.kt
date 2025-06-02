package com.arny.allfy.domain.usecase.post

import com.arny.allfy.domain.model.Comment
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetComments @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(
        postID: String,
        lastVisible: Comment? = null,
        limit: Int = 10
    ): Flow<Response<List<Comment>>> {
        return repository.getComments(postID, lastVisible, limit)
    }
}