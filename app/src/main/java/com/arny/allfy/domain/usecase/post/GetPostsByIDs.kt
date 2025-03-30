package com.arny.allfy.domain.usecase.post

import com.arny.allfy.domain.repository.PostRepository
import javax.inject.Inject

class GetPostsByIDs @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(postsIDs: List<String>) = repository.getPostsByIDs(postsIDs)
}