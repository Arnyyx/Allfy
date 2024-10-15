package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.usecase.Post.PostUseCases
import com.arny.allfy.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val postUseCases: PostUseCases
) : ViewModel() {

    private val _postData = mutableStateOf<Response<List<Post>>>(Response.Loading)
    val postData: State<Response<List<Post>>> = _postData

    private val _uploadPostSate = mutableStateOf<Response<Boolean>>(Response.Success(false))
    val uploadPostSate: State<Response<Boolean>> = _uploadPostSate

    fun getAllPosts(userID: String) {
        viewModelScope.launch {
            postUseCases.getAllPosts(userID).collect {
                _postData.value = it
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
}