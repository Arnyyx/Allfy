package com.arny.allfy.domain.usecase.post

import android.net.Uri
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AddComment @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(
        postID: String,
        commentOwnerID: String,
        content: String,
        parentCommentID: String?,
        imageUri: Uri?
    ): Flow<Response<Boolean>> {
        return repository.addComment(postID, commentOwnerID, content, parentCommentID, imageUri)
    }
}