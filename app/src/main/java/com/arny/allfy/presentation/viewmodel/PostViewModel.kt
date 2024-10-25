package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.Comment
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.usecase.Post.PostUseCases
import com.arny.allfy.presentation.state.PostState
import com.arny.allfy.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val postUseCases: PostUseCases
) : ViewModel() {
    private val _getFeedPostsState = MutableStateFlow(PostState())
    val getFeedPostsState: StateFlow<PostState> = _getFeedPostsState.asStateFlow()

    private var lastVisiblePost: Post? = null
    private var isLoadingMore = false

    fun getFeedPosts(currentUserID: String) {
        if (_getFeedPostsState.value.endReached || isLoadingMore) return

        isLoadingMore = true
        _getFeedPostsState.value = _getFeedPostsState.value.copy(isLoading = true, error = "")

        viewModelScope.launch {
            postUseCases.getFeedPosts(currentUserID, lastVisiblePost).collect { response ->
                when (response) {
                    is Response.Loading -> {
                        _getFeedPostsState.value = _getFeedPostsState.value.copy(isLoading = true)
                    }

                    is Response.Success -> {
                        val newPosts = response.data
                        lastVisiblePost = newPosts.lastOrNull()
                        _getFeedPostsState.value = _getFeedPostsState.value.copy(
                            isLoading = false,
                            posts = _getFeedPostsState.value.posts + newPosts,
                            endReached = newPosts.size < 10
                        )
                        isLoadingMore = false
                    }

                    is Response.Error -> {
                        _getFeedPostsState.value = _getFeedPostsState.value.copy(
                            isLoading = false,
                            error = response.message
                        )
                        isLoadingMore = false
                    }
                }
            }
        }
    }

    private val _uploadPostSate = mutableStateOf<Response<Boolean>>(Response.Success(false))
    val uploadPostSate: State<Response<Boolean>> = _uploadPostSate

    fun uploadPost(post: Post, imageUris: List<Uri>) {
        viewModelScope.launch {
            postUseCases.uploadPost(post, imageUris).collect {
                _uploadPostSate.value = it
            }
        }
    }

    private val _postsState = MutableStateFlow<Response<Map<String, Post>>>(Response.Loading)
    val postsState: StateFlow<Response<Map<String, Post>>> = _postsState.asStateFlow()

    private val loadedPosts = mutableMapOf<String, Post>()

    fun getPostByID(postID: String) {
        viewModelScope.launch {
            postUseCases.getPostByID(postID).collect { response ->
                when (response) {
                    is Response.Success -> {
                        response.data?.let { post ->
                            loadedPosts[postID] = post
                            _postsState.value = Response.Success(loadedPosts.toMap())
                        }
                    }

                    is Response.Error -> {
                        _postsState.value = response
                    }

                    is Response.Loading -> {
                        _postsState.value = response
                    }
                }
            }
        }
    }

    fun getPostDetail(postID: String): Post? {
        return loadedPosts[postID] ?: runBlocking {
            var resultPost: Post? = null
            postUseCases.getPostByID(postID).collect { response ->
                when (response) {
                    is Response.Success -> {
                        resultPost = response.data
                        loadedPosts[postID] = resultPost!!
                    }

                    is Response.Error -> {
                        _postsState.value = response
                    }

                    is Response.Loading -> {
                        _postsState.value = response
                    }
                }
            }
            resultPost
        }
    }

    private val _postsLikeState = mutableStateOf<Response<Boolean>>(Response.Success(false))
    val postsLikeState: State<Response<Boolean>> = _postsLikeState

    fun toggleLikePost(post: Post, userID: String) {
        viewModelScope.launch {
            postUseCases.toggleLikePost(post, userID).collect { response ->
                if (response is Response.Success) {
                    val updatedPosts = _getFeedPostsState.value.posts.map { currentPost ->
                        if (currentPost.postID == post.postID) {
                            val isLiked = post.likes.contains(userID)
                            val updatedLikes = if (isLiked) {
                                post.likes.filterNot { it == userID }
                            } else {
                                post.likes + userID
                            }
                            currentPost.copy(likes = updatedLikes)
                        } else {
                            currentPost
                        }
                    }
                    _getFeedPostsState.value = _getFeedPostsState.value.copy(posts = updatedPosts)
                }
                _postsLikeState.value = response
            }
        }
    }

    //Comment
    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    fun loadComments(postID: String) {
        viewModelScope.launch {
            postUseCases.getComments(postID).collect { response ->
                when (response) {
                    is Response.Error -> {
                        _comments.value = emptyList()
                    }

                    Response.Loading -> {
                        _comments.value = emptyList()
                    }

                    is Response.Success -> {
                        _comments.value = response.data
                    }
                }
            }

        }
    }

    private val _addCommentState = mutableStateOf<Response<Boolean>>(Response.Success(false))
    val addCommentState: State<Response<Boolean>> = _addCommentState

    fun addComment(postID: String, userID: String, content: String) {
        viewModelScope.launch {
            postUseCases.addComment(postID, userID, content).collect { response ->
                _addCommentState.value = response
            }
        }
    }

}