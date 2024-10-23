package com.arny.allfy.domain.usecase.Post

import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.repository.PostRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import okhttp3.internal.wait

//class ToggleLikePostUseCase(private val postRepository: PostRepository) {
//    operator fun invoke(postID: String, userID: String): Flow<Response<Post>> = flow {
//        emit(Response.Loading)
//        postRepository.getPostByID(postID).collect { response ->
//            when (response) {
//                is Response.Success -> {
//                    val post = response.data
//                    val isLiked = post.likes.contains(userID)
//
//                    val updatedLikes = if (isLiked) {
//                        post.likes - userID
//                    } else {
//                        post.likes + userID
//                    }
//
//                    val updatedPost = post.copy(likes = updatedLikes)
//                    postRepository.updatePost(updatedPost)
//                    emit(Response.Success(updatedPost))
//                }
//
//                is Response.Error -> {
//                    emit(Response.Error(response.message))
//                }
//
//                is Response.Loading -> {
//                    emit(Response.Loading)
//                }
//            }
//        }
//    }
//}

class ToggleLikePostUseCase(private val postRepository: PostRepository) {
    operator fun invoke(postID: String, userID: String): Flow<Response<Post>> = flow {
        emit(Response.Loading)

        // Lấy bài đăng một lần
        val response = postRepository.getPostByID(postID).first() // Sử dụng first() để lấy giá trị đầu tiên và không subscribe liên tục
        when (response) {
            is Response.Success -> {
                val post = response.data
                val isLiked = post.likes.contains(userID)

                // Cập nhật danh sách likes
                val updatedLikes = if (isLiked) {
                    post.likes - userID
                } else {
                    post.likes + userID
                }

                // Tạo bài đăng mới với danh sách likes đã cập nhật
                val updatedPost = post.copy(likes = updatedLikes)

                // Cập nhật bài đăng trong Firestore
                postRepository.updatePost(updatedPost)

                // Emit bài đăng đã cập nhật
                emit(Response.Success(updatedPost))
            }
            is Response.Error -> {
                emit(Response.Error(response.message))
            }
            is Response.Loading -> {
                emit(Response.Loading)
            }
        }
    }
}


