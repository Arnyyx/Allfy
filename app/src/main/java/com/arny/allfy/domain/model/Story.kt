package com.arny.allfy.domain.model

import com.google.firebase.Timestamp

data class Story(
    val storyID: String = "",
    val userID: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "", // "image" hoặc "video"
    val caption: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val duration: Long = 86400, // 24 giờ (thời gian tồn tại của story)
    val imageDuration: Long? = 5000, // 5 giây cho ảnh
    val maxVideoDuration: Long? = 60000, // 60 giây cho video
    val views: List<String> = emptyList(),
    val viewCount: Long = 0,
    val privacy: String = "public" // "public", "friends", "private"
)