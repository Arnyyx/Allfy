package com.arny.allfy.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Post(
    val postID: String = "",
    val postOwnerID: String = "",
    @get:Exclude val postOwner: User = User(),
    val mediaItems: List<MediaItem> = emptyList(),
    val caption: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val likes: List<String> = emptyList(),
    val commentCount: Int = 0,
    val keywords: List<String> = emptyList(),
    @get:Exclude val score: Float = 1F,
    @get:Exclude val reason: String = ""
)

data class MediaItem(
    val url: String = "",
    val mediaType: String = "image",
    val thumbnailUrl: String? = null
)