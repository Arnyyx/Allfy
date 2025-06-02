package com.arny.allfy.presentation.state

import com.arny.allfy.domain.model.Comment
import com.arny.allfy.domain.model.Post
import com.arny.allfy.utils.Response

data class PostState(
    val feedPostsState: Response<List<Post>> = Response.Idle,
    val uploadPostState: Response<Boolean> = Response.Idle,
    val deletePostState: Response<Boolean> = Response.Idle,
    val getPostState: Response<Post> = Response.Idle,
    val getPostsState: Response<List<Post>> = Response.Idle,
    val likePostState: Response<Boolean> = Response.Idle,
    val loadCommentsState: Response<List<Comment>> = Response.Idle,
    val addCommentState: Response<Boolean> = Response.Idle,
    val likeCommentState: Response<Boolean> = Response.Idle,
    val logViewState: Response<Boolean> = Response.Idle
) {
    data class Success<T>(
        val data: T,
        val hasMore: Boolean = false
    )
}