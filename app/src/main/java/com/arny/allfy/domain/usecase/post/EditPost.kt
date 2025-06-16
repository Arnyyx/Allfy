package com.arny.allfy.domain.usecase.post

import android.net.Uri
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EditPost @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(
        postID: String,
        userID: String,
        newCaption: String,
        newImageUris: List<Uri>,
        mediaItemsToRemove: List<String>
    ): Flow<Response<Boolean>> {
        return postRepository.editPost(
            postID, userID, newCaption, newImageUris, mediaItemsToRemove
        )
    }
}