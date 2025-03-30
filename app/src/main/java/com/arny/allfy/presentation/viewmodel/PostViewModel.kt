package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.Comment
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.usecase.post.PostUseCases
import com.arny.allfy.domain.usecase.user.UserUseCases
import com.arny.allfy.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val postUseCases: PostUseCases
) : ViewModel() {
    private val _postState = MutableStateFlow(PostState())
    val postState: StateFlow<PostState> = _postState.asStateFlow()

    fun getFeedPosts(currentUserID: String) {
        viewModelScope.launch {
            postUseCases.getFeedPosts(currentUserID).collect { response ->
                when (response) {
                    is Response.Loading -> _postState.update { it.copy(isLoadingFeed = true) }
                    is Response.Success -> {
                        _postState.update {
                            it.copy(
                                isLoadingFeed = false,
                                feedPosts = response.data,
                            )
                        }
                    }

                    is Response.Error -> _postState.update {
                        it.copy(
                            isLoadingFeed = false,
                            feedPostsError = response.message
                        )
                    }
                }
            }
        }
    }

    val _uploadState = mutableStateOf(Response.Success(false))

    fun uploadPost(post: Post, imageUris: List<Uri>) {
        viewModelScope.launch {
            postUseCases.uploadPost(post, imageUris).collect { response ->
                when (response) {
                    is Response.Loading -> _postState.update { it.copy(isUploadingPost = true) }
                    is Response.Success -> {
                        _postState.update { it.copy(isUploadingPost = false) }
                    }

                    is Response.Error -> _postState.update { it.copy(uploadPostError = response.message) }
                }
            }
        }
    }

    fun deletePost(postID: String, currentUserID: String) {
        viewModelScope.launch {
            postUseCases.deletePost(postID, currentUserID).collect { response ->
                when (response) {
                    is Response.Loading -> _postState.update { it.copy(isDeletingPost = true) }
                    is Response.Success -> {
                        _postState.update { it.copy(isDeletingPost = false) }
                    }

                    is Response.Error -> _postState.update { it.copy(deletePostError = response.message) }
                }
            }
        }
    }

    fun getPostByID(postId: String) {
        viewModelScope.launch {
            postUseCases.getPostByID(postId).collect { response ->
                when (response) {
                    is Response.Loading -> _postState.update { it.copy(isLoadingPost = true) }
                    is Response.Error -> _postState.update { it.copy(loadPostError = response.message) }
                    is Response.Success -> {
                        _postState.update { it.copy(isLoadingPost = false, post = response.data) }
                    }

                }
            }
        }
    }

    fun getPostsByIds(postIds: List<String>) {
        viewModelScope.launch {
            postUseCases.getPostsByIds(postIds).collect { response ->
                when (response) {
                    is Response.Loading -> _postState.update { it.copy(isLoadingPosts = true) }
                    is Response.Error -> _postState.update { it.copy(loadPostsError = response.message) }
                    is Response.Success -> {
                        _postState.update {
                            it.copy(
                                isLoadingPosts = false,
                                posts = response.data
                            )
                        }
                    }
                }
            }
        }
    }

    fun toggleLikePost(post: Post, userID: String) {
        viewModelScope.launch {
            postUseCases.toggleLikePost(post, userID).collect { response ->
                when (response) {
                    is Response.Loading -> _postState.update { it.copy(isLikingPost = true) }
                    is Response.Success -> {
                        _postState.update { it.copy(isLikingPost = false) }
                    }

                    is Response.Error -> _postState.update { it.copy(likePostError = response.message) }
                }
            }
        }
    }

    fun loadComments(postID: String) {
        viewModelScope.launch {
            postUseCases.getComments(postID).collect { response ->
                when (response) {
                    is Response.Loading -> _postState.update { it.copy(isLoadingComments = true) }
                    is Response.Success -> {
                        _postState.update {
                            it.copy(
                                isLoadingComments = false,
                                comments = response.data
                            )
                        }
                    }

                    is Response.Error -> _postState.update { it.copy(loadCommentsError = response.message) }
                }
            }
        }
    }

    fun addComment(
        postID: String,
        userID: String,
        content: String,
        parentCommentID: String? = null
    ) {
        viewModelScope.launch {
            postUseCases.addComment(postID, userID, content, parentCommentID).collect { response ->
                when (response) {
                    is Response.Loading -> _postState.update { it.copy(isAddingComment = true) }
                    is Response.Success -> {
                        _postState.update { it.copy(isAddingComment = false) }
                        loadComments(postID)
                    }

                    is Response.Error -> _postState.update { it.copy(addCommentError = response.message) }
                }
            }
        }
    }

    fun toggleLikeComment(postID: String, comment: Comment, userID: String) {
        viewModelScope.launch {
            postUseCases.toggleLikeComment(postID, comment.commentID, userID).collect { response ->
                when (response) {
                    is Response.Loading -> _postState.update { it.copy(isLikingComment = true) }
                    is Response.Error -> _postState.update { it.copy(likeCommentError = response.message) }
                    is Response.Success -> {
                        _postState.update { it.copy(isLikingComment = false) }
                    }
                }
            }
        }
    }

    fun clearPostState() {
        _postState.value = PostState()
    }
}

data class PostState(
    val isLoadingFeed: Boolean = false,
    val feedPosts: List<Post> = emptyList(),
    val feedPostsError: String = "",

    val isUploadingPost: Boolean = false,
    val uploadPostError: String = "",

    val isDeletingPost: Boolean = false,
    val deletePostError: String = "",

    val isLoadingPost: Boolean = false,
    val post: Post = Post(),
    val loadPostError: String = "",

    val isLoadingPosts: Boolean = false,
    val posts: List<Post> = emptyList(),
    val loadPostsError: String = "",

    val isLikingPost: Boolean = false,
    val likePostError: String = "",

    val isLoadingComments: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val loadCommentsError: String = "",

    val isAddingComment: Boolean = false,
    val addCommentError: String = "",

    val isLikingComment: Boolean = false,
    val likeCommentError: String = ""
)