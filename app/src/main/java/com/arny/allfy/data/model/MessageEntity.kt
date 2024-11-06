package com.arny.allfy.data.model

data class MessageEntity(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val type: String = "TEXT"
)