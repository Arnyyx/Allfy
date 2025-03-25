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
                        _users.value = _users.value + newUsers
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

    private val _postsState = MutableStateFlow<Response<Map<String, Post>>>(Response.Loading)
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

                    is Response.Loading -> {}
                }
            }
        }
    }

    // Trạng thái cho Comments (bao gồm cả Reply)
    private val _comments = MutableStateFlow<Response<List<Comment>>>(Response.Loading)
    val comments: StateFlow<Response<List<Comment>>> = _comments.asStateFlow()

    // Tải Comments cho một Post
    fun loadComments(postID: String) {
        viewModelScope.launch {
            postUseCases.getComments(postID).collect { response ->
                _comments.value = response
            }
        }
    }

    // Trạng thái cho việc thêm Comment/Reply
    private val _addCommentState = mutableStateOf<Response<Boolean>>(Response.Success(false))
    val addCommentState: State<Response<Boolean>> = _addCommentState

    // Thêm Comment hoặc Reply (với parentCommentID để xác định Reply)
    fun addComment(
        postID: String,
        userID: String,
        content: String,
        parentCommentID: String? = null
    ) {
        viewModelScope.launch {
            postUseCases.addComment(postID, userID, content, parentCommentID).collect { response ->
                _addCommentState.value = response
                if (response is Response.Success && response.data) {
                    // Sau khi thêm thành công, tải lại danh sách Comment
                    loadComments(postID)
                    _addCommentState.value = Response.Success(false) // Reset state
                }
            }
        }
    }

    private val _commentLikeLoadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val commentLikeLoadingStates: StateFlow<Map<String, Boolean>> =
        _commentLikeLoadingStates.asStateFlow()

    fun toggleLikeComment(postID: String, comment: Comment, userID: String) {
        viewModelScope.launch {
            _commentLikeLoadingStates.value += (comment.commentID to true)
            postUseCases.toggleLikeComment(postID, comment.commentID, userID).collect { response ->
                when (response) {
                    is Response.Success -> {
                        val updatedComment = response.data
                        _comments.value = when (val currentComments = _comments.value) {
                            is Response.Success -> {
                                Response.Success(
                                    currentComments.data.map {
                                        if (it.commentID == updatedComment.commentID) updatedComment else it
                                    }
                                )
                            }

                            else -> _comments.value
                        }

                        _getFeedPostsState.value = _getFeedPostsState.value.copy(
                            posts = _getFeedPostsState.value.posts.map { post ->
                                if (post.postID == postID) {
                                    post.copy(
                                        comments = post.comments.map {
                                            if (it.commentID == updatedComment.commentID) updatedComment else it
                                        }
                                    )
                                } else post
                            }
                        )

                        _currentPost.value?.let { current ->
                            if (current.postID == postID) {
                                _currentPost.value = current.copy(
                                    comments = current.comments.map {
                                        if (it.commentID == updatedComment.commentID) updatedComment else it
                                    }
                                )
                            }
                        }

                        _commentLikeLoadingStates.value -= comment.commentID
                    }

                    is Response.Error -> {
                        _getFeedPostsState.value = _getFeedPostsState.value.copy(
                            error = response.message
                        )
                        _commentLikeLoadingStates.value -= comment.commentID
                    }

                    is Response.Loading -> {}
                }
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
        _commentLikeLoadingStates.value = emptyMap()
        _currentPost.value = null
        _comments.value = Response.Loading
        _addCommentState.value = Response.Success(false)
    }
}