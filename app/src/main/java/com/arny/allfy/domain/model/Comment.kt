package com.arny.allfy.domain.model

import com.arny.allfy.utils.toTimeAgo
import com.google.firebase.Timestamp

data class Comment(
    val commentID: String = "",
    val commentOwnerID: String = "",
    val commentOwnerUserName: String = "",
    val commentOwnerProfilePicture: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now()
) {
    val timeAgo: String get() = timestamp.toTimeAgo()
}