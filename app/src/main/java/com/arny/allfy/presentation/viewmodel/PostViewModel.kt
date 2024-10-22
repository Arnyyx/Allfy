package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.usecase.Post.PostUseCases
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
    private val postUseCases: PostUseCases
) : ViewModel() {

    private val _getPostState = mutableStateOf<Response<Post>>(Response.Loading)
    val getPostState: State<Response<Post>> = _getPostState

    private val _uploadPostSate = mutableStateOf<Response<Boolean>>(Response.Success(false))
    val uploadPostSate: State<Response<Boolean>> = _uploadPostSate

    private val _getAllPostsState = MutableStateFlow(PostState())
    val getAllPostsState: StateFlow<PostState> = _getAllPostsState.asStateFlow()

    private var lastVisiblePost: Post? = null
    private var isLoadingMore = false

    fun getAllPosts(userID: String) {
        if (_getAllPostsState.value.endReached || isLoadingMore) return

        isLoadingMore = true
        _getAllPostsState.value = _getAllPostsState.value.copy(isLoading = true, error = "")

        viewModelScope.launch {
            postUseCases.getAllPosts(userID, lastVisiblePost).collect { response ->
                when (response) {
                    is Response.Loading -> {
                        _getAllPostsState.value = _getAllPostsState.value.copy(isLoading = true)
                    }

                    is Response.Success -> {
                        val newPosts = response.data
                        lastVisiblePost = newPosts.lastOrNull()
                        _getAllPostsState.value = _getAllPostsState.value.copy(
                            isLoading = false,
                            posts = _getAllPostsState.value.posts + newPosts,
                            endReached = newPosts.size < 10 // Giả định limit = 10
                        )
                        isLoadingMore = false
                    }

                    is Response.Error -> {
                        _getAllPostsState.value = _getAllPostsState.value.copy(
                            isLoading = false,
                            error = response.message
                        )
                        isLoadingMore = false
                    }
                }
            }
        }
    }


    fun uploadPost(post: Post, imageUris: List<Uri>) {
        viewModelScope.launch {
            postUseCases.uploadPost(post, imageUris).collect {
                _uploadPostSate.value = it
            }
        }
    }

//    fun getPost(postID: String) {
//        viewModelScope.launch {
//            postUseCases.getPost(postID).collect {
//                _getPostState.value = it
//            }
//        }
//    }

    private val _postsState = MutableStateFlow<Response<Map<String, Post>>>(Response.Loading)
    val postsState: StateFlow<Response<Map<String, Post>>> = _postsState.asStateFlow()

    private val loadedPosts = mutableMapOf<String, Post>()

    fun getPost(postId: String) {
        viewModelScope.launch {
            postUseCases.getPost(postId).collect { response ->
                when (response) {
                    is Response.Success -> {
                        response.data?.let { post ->
                            loadedPosts[postId] = post
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


}