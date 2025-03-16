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
import com.arny.allfy.presentation.state.PostState
import com.arny.allfy.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val postUseCases: PostUseCases,
    private val userUseCases: UserUseCases
) : ViewModel() {
    private val _getFeedPostsState = MutableStateFlow(PostState())
    val getFeedPostsState: StateFlow<PostState> = _getFeedPostsState.asStateFlow()
    private val _users = MutableStateFlow<Map<String, User>>(emptyMap())
    val users: StateFlow<Map<String, User>> = _users.asStateFlow()

    private var lastVisiblePost: Post? = null
    private var isLoadingMore = false

    fun getFeedPosts(currentUserID: String, forceRefresh: Boolean = false) {
        if (!forceRefresh && (_getFeedPostsState.value.endReached || isLoadingMore)) {
            return
        }

        if (forceRefresh) {
            lastVisiblePost = null
            isLoadingMore = false
            _getFeedPostsState.value = PostState(isLoading = true)
        } else if (_getFeedPostsState.value.posts.isNotEmpty()) {
            isLoadingMore = true
        } else {
            _getFeedPostsState.value = _getFeedPostsState.value.copy(isLoading = true)
        }

        viewModelScope.launch {
            postUseCases.getFeedPosts(currentUserID, lastVisiblePost).collect { response ->
                when (response) {
                    is Response.Loading -> {
                        if (forceRefresh || _getFeedPostsState.value.posts.isEmpty()) {
                            _getFeedPostsState.value =
                                _getFeedPostsState.value.copy(isLoading = true)
                        }
                    }

                    is Response.Success -> {
                        val newPosts = response.data
                        lastVisiblePost = newPosts.lastOrNull()

                        val userIds = newPosts.map { it.postOwnerID }.distinct()
                        fetchUsers(userIds)

                        _getFeedPostsState.value = _getFeedPostsState.value.copy(
                            isLoading = false,
                            posts = if (forceRefresh || _getFeedPostsState.value.posts.isEmpty()) {
                                newPosts
                            } else {
                                _getFeedPostsState.value.posts + newPosts
                            },
                            endReached = newPosts.size < 10,
                            error = ""
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


    private fun fetchUsers(userIds: List<String>) {
        viewModelScope.launch {
            userUseCases.getUsersByIDs(userIds).collect { response ->
                when (response) {
                    is Response.Success -> {
                        val newUsers = response.data.associateBy { it.userId }
                        _users.value = _users.value + newUsers // Cập nhật cache
                    }

                    is Response.Error -> {
                        _getFeedPostsState.value = _getFeedPostsState.value.copy(
                            error = response.message
                        )
                    }

                    is Response.Loading -> {}
                }
            }
        }
    }

    private val _uploadPostSate = MutableStateFlow<Response<Boolean>>(Response.Success(false))
    val uploadPostSate: StateFlow<Response<Boolean>> = _uploadPostSate

    fun uploadPost(post: Post, imageUris: List<Uri>) {
        viewModelScope.launch {
            postUseCases.uploadPost(post, imageUris).collect {
                _uploadPostSate.value = it
                if (it is Response.Success) {
                    _uploadPostSate.value = Response.Success(false)
                }
            }
        }
    }

    private val _deletePostState = MutableStateFlow<Response<Boolean>>(Response.Success(false))
    val deletePostState: StateFlow<Response<Boolean>> = _deletePostState

    fun deletePost(postID: String, currentUserID: String) {
        viewModelScope.launch {
            postUseCases.deletePost(postID, currentUserID).collect {
                _deletePostState.value = it
            }
        }
    }

    private val _postsState =
        MutableStateFlow<Response<Map<String, Post>>>(Response.Loading)
    val postsState: StateFlow<Response<Map<String, Post>>> = _postsState.asStateFlow()

    private val loadedPosts = mutableMapOf<String, Post>()

    fun getPostByID(postId: String) {
        viewModelScope.launch {
            postUseCases.getPostByID(postId).collect { response ->
                when (response) {
                    is Response.Success -> {
                        val currentPosts =
                            (_postsState.value as? Response.Success)?.data ?: emptyMap()
                        _postsState.value =
                            Response.Success(currentPosts + (postId to response.data))
                    }

                    is Response.Error -> _postsState.value = Response.Error(response.message)
                    is Response.Loading -> _postsState.value = Response.Loading
                }
            }
        }
    }

    private val _likeLoadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val likeLoadingStates: StateFlow<Map<String, Boolean>> = _likeLoadingStates.asStateFlow()

    private val _currentPost = MutableStateFlow<Post?>(null)
    val currentPost: StateFlow<Post?> = _currentPost.asStateFlow()

    fun toggleLikePost(post: Post, userID: String) {
        viewModelScope.launch {
            _likeLoadingStates.value += (post.postID to true)

            postUseCases.toggleLikePost(post, userID).collect { response ->
                when (response) {
                    is Response.Success -> {
                        val updatedPost = response.data
                        _getFeedPostsState.value = _getFeedPostsState.value.copy(
                            posts = _getFeedPostsState.value.posts.map {
                                if (it.postID == updatedPost.postID) updatedPost else it
                            }
                        )
                        _currentPost.value = updatedPost
                        _likeLoadingStates.value -= post.postID
                    }

                    is Response.Error -> {
                        _getFeedPostsState.value = _getFeedPostsState.value.copy(
                            error = response.message
                        )
                        _likeLoadingStates.value -= post.postID
                    }

                    is Response.Loading -> {
                    }
                }
            }
        }
    }


    //Comment
    private val _comments = MutableStateFlow<Response<List<Comment>>>(Response.Loading)
    val comments: StateFlow<Response<List<Comment>>> = _comments.asStateFlow()

    fun loadComments(postID: String) {
        viewModelScope.launch {
            postUseCases.getComments(postID).collect { response ->
                _comments.value = response
            }
        }
    }

    private val _addCommentState =
        mutableStateOf<Response<Boolean>>(Response.Success(false))
    val addCommentState: State<Response<Boolean>> = _addCommentState

    fun addComment(postID: String, userID: String, content: String) {
        viewModelScope.launch {
            postUseCases.addComment(postID, userID, content).collect { response ->
                _addCommentState.value = response
            }
        }
    }

    fun clear() {
        _getFeedPostsState.value = PostState()
        lastVisiblePost = null
        isLoadingMore = false
        _uploadPostSate.value = Response.Success(false)
        loadedPosts.clear()
        _postsState.value = Response.Loading
        _likeLoadingStates.value = emptyMap()
        _currentPost.value = null
        _comments.value = Response.Loading
        _addCommentState.value = Response.Success(false)
    }


}