package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.Comment
import com.arny.allfy.domain.model.MediaItem
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.usecase.post.PostUseCases
import com.arny.allfy.presentation.state.PostState
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.getDataOrNull
import com.arny.allfy.utils.mapSuccess
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

    private val _loadingPosts = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val loadingPosts: StateFlow<Map<String, Set<String>>> = _loadingPosts.asStateFlow()

    private var lastLoadedComment: Comment? = null
    private val commentsPerPage = 10

    fun getFeedPosts(currentUserID: String) {
        viewModelScope.launch {
            postUseCases.getFeedPosts(currentUserID).collect { response ->
                _postState.update { it.copy(feedPostsState = response) }
            }
        }
    }

    fun uploadPost(post: Post, imageUris: List<Uri>) {
        viewModelScope.launch {
            _loadingPosts.update {
                it + (post.postID to (it[post.postID] ?: emptySet()) + "upload")
            }
            postUseCases.uploadPost(post, imageUris).collect { response ->
                _postState.update { it.copy(uploadPostState = response.mapSuccess { true }) }
                _loadingPosts.update {
                    it + (post.postID to (it[post.postID] ?: emptySet()) - "upload")
                }
            }
        }
    }

    fun deletePost(postID: String, currentUserID: String) {
        viewModelScope.launch {
            _loadingPosts.update { it + (postID to (it[postID] ?: emptySet()) + "delete") }
            postUseCases.deletePost(postID, currentUserID).collect { response ->
                _postState.update { it.copy(deletePostState = response.mapSuccess { true }) }
                _loadingPosts.update { it + (postID to (it[postID] ?: emptySet()) - "delete") }
            }
        }
    }

    fun getPostByID(postId: String) {
        viewModelScope.launch {
            postUseCases.getPostByID(postId).collect { response ->
                _postState.update { it.copy(getPostState = response) }
            }
        }
    }

    fun getPostsByIds(postIds: List<String>) {
        viewModelScope.launch {
            postUseCases.getPostsByIds(postIds).collect { response ->
                _postState.update { it.copy(getPostsState = response) }
            }
        }
    }

    fun toggleLikePost(post: Post, userId: String) {
        viewModelScope.launch {
            _loadingPosts.update { it + (post.postID to (it[post.postID] ?: emptySet()) + "like") }
            _postState.update { currentState ->
                val currentFeedPosts = currentState.feedPostsState.getDataOrNull() ?: emptyList()
                val updatedFeedPosts = currentFeedPosts.map { p ->
                    if (p.postID == post.postID) {
                        val updatedLikes = if (p.likes.contains(userId)) {
                            p.likes - userId
                        } else {
                            p.likes + userId
                        }
                        p.copy(likes = updatedLikes)
                    } else {
                        p
                    }
                }
                val updatedGetPostState =
                    if (currentState.getPostState.getDataOrNull()?.postID == post.postID) {
                        val currentPost = currentState.getPostState.getDataOrNull()
                        currentPost?.let {
                            val updatedLikes = if (it.likes.contains(userId)) {
                                it.likes - userId
                            } else {
                                it.likes + userId
                            }
                            Response.Success(it.copy(likes = updatedLikes))
                        } ?: currentState.getPostState
                    } else {
                        currentState.getPostState
                    }
                currentState.copy(
                    feedPostsState = Response.Success(updatedFeedPosts),
                    getPostState = updatedGetPostState
                )
            }

            postUseCases.toggleLikePost(post, userId).collect { response ->
                _postState.update { it.copy(likePostState = response.mapSuccess { true }) }
                _loadingPosts.update {
                    it + (post.postID to (it[post.postID] ?: emptySet()) - "like")
                }
            }
        }
    }

    fun loadComments(postID: String, reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) {
                lastLoadedComment = null
                _postState.update { it.copy(loadCommentsState = Response.Loading) }
            }

            postUseCases.getComments(postID, lastLoadedComment, commentsPerPage)
                .collect { response ->
                    when (response) {
                        is Response.Success -> {
                            val newComments = response.data

                            if (newComments.isNotEmpty()) {
                                lastLoadedComment = newComments.lastOrNull()
                            }

                            val currentComments = if (reset) {
                                emptyList()
                            } else {
                                (_postState.value.loadCommentsState as? Response.Success)?.data
                                    ?: emptyList()
                            }

                            val allComments = if (reset) {
                                newComments
                            } else {
                                val existingIds = currentComments.map { it.commentID }.toSet()
                                val filteredNewComments =
                                    newComments.filter { it.commentID !in existingIds }
                                currentComments + filteredNewComments
                            }

                            val hasMore = newComments.size == commentsPerPage

                            _postState.update { currentState ->
                                currentState.copy(
                                    loadCommentsState = Response.Success(
                                        data = allComments,
                                        hasMore = hasMore
                                    )
                                )
                            }
                        }

                        is Response.Error -> {
                            if (reset) {
                                _postState.update { it.copy(loadCommentsState = response) }
                            }
                        }

                        is Response.Loading -> {
                            if (reset) {
                                _postState.update { it.copy(loadCommentsState = response) }
                            }
                        }

                        else -> {
                            if (reset) {
                                _postState.update { it.copy(loadCommentsState = response) }
                            }
                        }
                    }
                }
        }
    }

    fun addComment(
        postID: String,
        userID: String,
        content: String,
        parentCommentID: String? = null,
        imageUri: Uri? = null
    ) {
        viewModelScope.launch {
            _loadingPosts.update { it + (postID to (it[postID] ?: emptySet()) + "comment") }
            _postState.update { currentState ->
                val currentFeedPosts = currentState.feedPostsState.getDataOrNull() ?: emptyList()
                val updatedFeedPosts = currentFeedPosts.map { post ->
                    if (post.postID == postID) {
                        post.copy(commentCount = post.commentCount + 1)
                    } else {
                        post
                    }
                }
                val updatedGetPostState =
                    if (currentState.getPostState.getDataOrNull()?.postID == postID) {
                        val currentPost = currentState.getPostState.getDataOrNull()
                        currentPost?.let {
                            Response.Success(it.copy(commentCount = it.commentCount + 1))
                        } ?: currentState.getPostState
                    } else {
                        currentState.getPostState
                    }
                currentState.copy(
                    feedPostsState = Response.Success(updatedFeedPosts),
                    getPostState = updatedGetPostState
                )
            }

            postUseCases.addComment(postID, userID, content, parentCommentID, imageUri)
                .collect { response ->
                    _postState.update { it.copy(addCommentState = response.mapSuccess { true }) }
                    if (response is Response.Error) {
                        _postState.update { currentState ->
                            val currentFeedPosts =
                                currentState.feedPostsState.getDataOrNull() ?: emptyList()
                            val revertedFeedPosts = currentFeedPosts.map { post ->
                                if (post.postID == postID) {
                                    post.copy(commentCount = post.commentCount - 1)
                                } else {
                                    post
                                }
                            }
                            val revertedGetPostState =
                                if (currentState.getPostState.getDataOrNull()?.postID == postID) {
                                    val currentPost = currentState.getPostState.getDataOrNull()
                                    currentPost?.let {
                                        Response.Success(it.copy(commentCount = it.commentCount - 1))
                                    } ?: currentState.getPostState
                                } else {
                                    currentState.getPostState
                                }
                            currentState.copy(
                                feedPostsState = Response.Success(revertedFeedPosts),
                                getPostState = revertedGetPostState
                            )
                        }
                    }
                    _loadingPosts.update { it + (postID to (it[postID] ?: emptySet()) - "comment") }
                }
        }
    }

    fun toggleLikeComment(postID: String, comment: Comment, userID: String) {
        viewModelScope.launch {
            _loadingPosts.update { it + (postID to (it[postID] ?: emptySet()) + "likeComment") }
            _postState.update { currentState ->
                val currentComments =
                    (currentState.loadCommentsState as? Response.Success)?.data ?: emptyList()
                val updatedComments = currentComments.map { c ->
                    if (c.commentID == comment.commentID) {
                        val updatedLikes = if (c.likes.contains(userID)) {
                            c.likes - userID
                        } else {
                            c.likes + userID
                        }
                        c.copy(likes = updatedLikes)
                    } else {
                        c
                    }
                }
                // Giữ nguyên hasMore từ state hiện tại
                val currentHasMore =
                    (currentState.loadCommentsState as? Response.Success)?.hasMore ?: false
                currentState.copy(
                    loadCommentsState = Response.Success(
                        data = updatedComments,
                        hasMore = currentHasMore
                    )
                )
            }

            postUseCases.toggleLikeComment(postID, comment.commentID, userID).collect { response ->
                _postState.update { it.copy(likeCommentState = response.mapSuccess { true }) }
                if (response is Response.Error) {
                    loadComments(postID)
                }
                _loadingPosts.update { it + (postID to (it[postID] ?: emptySet()) - "likeComment") }
            }
        }
    }

    fun logPostView(userID: String, postID: String) {
        viewModelScope.launch {
            postUseCases.logPostView(userID, postID).collect { response ->
                _postState.update { it.copy(logViewState = response.mapSuccess { true }) }
            }
        }
    }

    fun editPost(
        postID: String,
        userID: String,
        newCaption: String,
        newImageUris: List<Uri>,
        mediaItemsToRemove: List<String>
    ) {
        viewModelScope.launch {
            _loadingPosts.update { it + (postID to (it[postID] ?: emptySet()) + "edit") }

            val originalFeedPosts = _postState.value.feedPostsState.getDataOrNull() ?: emptyList()
            val originalGetPost = _postState.value.getPostState.getDataOrNull()
            _postState.update { currentState ->
                val updatedFeedPosts = originalFeedPosts.map { post ->
                    if (post.postID == postID) {
                        post.copy(
                            caption = newCaption,
                            mediaItems = post.mediaItems
                                .filterNot { it.url in mediaItemsToRemove } + newImageUris.map { uri ->
                                MediaItem(
                                    url = uri.toString(),
                                    mediaType = if (uri.toString()
                                            .contains("video")
                                    ) "video" else "image",
                                    thumbnailUrl = null
                                )
                            }
                        )
                    } else {
                        post
                    }
                }
                val updatedGetPostState = if (originalGetPost?.postID == postID) {
                    Response.Success(
                        originalGetPost.copy(
                            caption = newCaption,
                            mediaItems = originalGetPost.mediaItems
                                .filterNot { it.url in mediaItemsToRemove } + newImageUris.map { uri ->
                                MediaItem(
                                    url = uri.toString(),
                                    mediaType = if (uri.toString()
                                            .contains("video")
                                    ) "video" else "image",
                                    thumbnailUrl = null
                                )
                            }
                        )
                    )
                } else {
                    currentState.getPostState
                }
                currentState.copy(
                    feedPostsState = Response.Success(updatedFeedPosts),
                    getPostState = updatedGetPostState,
                    editPostState = Response.Loading
                )
            }

            postUseCases.editPost(postID, userID, newCaption, newImageUris, mediaItemsToRemove)
                .collect { response ->
                    _postState.update { it.copy(editPostState = response.mapSuccess { true }) }
                    if (response is Response.Error) {
                        _postState.update { currentState ->
                            currentState.copy(
                                feedPostsState = Response.Success(originalFeedPosts),
                                getPostState = if (originalGetPost != null) Response.Success(
                                    originalGetPost
                                ) else currentState.getPostState
                            )
                        }
                    }
                    _loadingPosts.update { it + (postID to (it[postID] ?: emptySet()) - "edit") }
                }
        }
    }

    fun resetFeedPostsState() {
        _postState.update { it.copy(feedPostsState = Response.Idle) }
    }

    fun resetUploadPostState() {
        _postState.update { it.copy(uploadPostState = Response.Idle) }
    }

    fun resetDeletePostState() {
        _postState.update { it.copy(deletePostState = Response.Idle) }
    }

    fun resetGetPostState() {
        _postState.update { it.copy(getPostState = Response.Idle) }
    }

    fun resetGetPostsState() {
        _postState.update { it.copy(getPostsState = Response.Idle) }
    }

    fun resetLikePostState() {
        _postState.update { it.copy(likePostState = Response.Idle) }
    }

    fun resetLoadCommentsState() {
        _postState.update { it.copy(loadCommentsState = Response.Idle) }
        lastLoadedComment = null
    }

    fun resetAddCommentState() {
        _postState.update { it.copy(addCommentState = Response.Idle) }
    }

    fun resetLikeCommentState() {
        _postState.update { it.copy(likeCommentState = Response.Idle) }
    }

    fun resetLogViewState() {
        _postState.update { it.copy(logViewState = Response.Idle) }
    }

    fun resetEditPostState() {
        _postState.update { it.copy(editPostState = Response.Idle) }
    }

    fun clearPostState() {
        _postState.value = PostState()
        _loadingPosts.value = emptyMap()
        lastLoadedComment = null
    }
}