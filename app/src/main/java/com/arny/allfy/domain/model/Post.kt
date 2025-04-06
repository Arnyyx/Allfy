package com.arny.allfy.domain.model

data class Post(
    val postID: String = "",
    val postOwnerID: String = "",
    val postOwnerUsername: String = "",
    val postOwnerImageUrl: String = "",
    val mediaItems: List<MediaItem> = emptyList(),
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var likes: List<String> = emptyList(),
    val commentCount: Int = 0,
    val keywords: List<String> = emptyList()
)

data class MediaItem(
    val url: String = "",
    val mediaType: String = "image",
    val thumbnailUrl: String? = null
)