package com.arny.allfy.domain.usecase.post

import android.net.Uri
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UploadPost @Inject constructor(
    private val repository: PostRepository
) {
    suspend operator fun invoke(post: Post, imageUris: List<Uri>): Flow<Response<Boolean>> {
        return repository.uploadPost(post, imageUris)
    }
}