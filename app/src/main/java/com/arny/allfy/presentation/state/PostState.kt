package com.arny.allfy.presentation.state

import com.arny.allfy.domain.model.Post

data class PostState(
    val isLoading: Boolean = false,
    val posts: List<Post> = emptyList(),
    val error: String = "",
    val endReached: Boolean = false
)