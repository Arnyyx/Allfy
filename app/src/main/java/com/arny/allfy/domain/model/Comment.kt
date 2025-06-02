package com.arny.allfy.domain.model

import com.arny.allfy.utils.toTimeAgo
import com.google.firebase.Timestamp

data class Comment(
    val commentID: String = "",
    val commentOwnerID: String = "",
    val commentOwnerUserName: String = "",
    val commentOwnerProfilePicture: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val likes: List<String> = emptyList(),
    val parentCommentID: String? = null
) {
    val timeAgo: String get() = timestamp.toTimeAgo()
}