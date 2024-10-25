package com.arny.allfy.domain.model

data class Comment(
    val commentID: String = "",
    val commentOwnerID: String = "",
    val commentOwnerUserName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)