package com.arny.allfy.domain.model

data class Post(
    val postID: String = "",
    val postOwnerID: String = "",
    val postOwnerUsername: String = "",
    val postOwnerImageUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var likes: List<String> = emptyList(),
    val comments: Int = 0
)
