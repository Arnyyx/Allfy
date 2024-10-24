package com.arny.allfy.domain.usecase.Post

import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.repository.PostRepository
import javax.inject.Inject

class GetFeedPosts @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(currentUser: String, lastVisible: Post? = null, limit: Int = 10) =
        repository.getFeedPosts(currentUser, lastVisible, limit)
}