package com.arny.allfy.domain.model

data class Post(
    val id: String = "",
    val userID: String = "",
    val username: String = "",
    val imageUrls: List<String> = emptyList(),
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val comments: Int = 0
)
