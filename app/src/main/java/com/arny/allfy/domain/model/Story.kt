package com.arny.allfy.domain.model

import com.google.firebase.Timestamp

data class Story(
    val storyID: String = "",
    val userID: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "", // "image" or "video"
    val caption: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val duration: Long = 86400, // Default 24 hours in seconds, but can be customized
    val imageDuration: Long? = 5000, // 5 seconds for images
    val maxVideoDuration: Long? = 60000, // 60 seconds for videos
    val views: List<String> = emptyList(),
    val viewCount: Long = 0,
    val privacy: String = "public", // "public", "friends", "private"
    val userName: String = "",
    val userProfilePicture: String = ""
)