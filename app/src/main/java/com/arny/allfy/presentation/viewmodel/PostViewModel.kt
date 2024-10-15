package com.arny.allfy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.arny.allfy.domain.usecase.Post.PostUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val postUseCases: PostUseCases
) : ViewModel() {

}