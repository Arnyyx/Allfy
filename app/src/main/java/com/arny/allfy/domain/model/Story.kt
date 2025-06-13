package com.arny.allfy.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Story(
    val storyID: String = "",
    val userID: String = "",
    @get:Exclude val storyOwner: User = User(),
    val mediaUrl: String = "",
    val mediaType: String = "",
    val duration: Long = 0L,
    val imageDuration: Long? = null,
    val maxVideoDuration: Long? = null,
    val timestamp: Timestamp? = null,
    val views: List<String> = emptyList(),
    val viewCount: Long = 0L
)