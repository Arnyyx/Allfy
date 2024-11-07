package com.arny.allfy.domain.model

data class Conversation(
    val id: String = "",
    val otherUserID: String = "",
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val timestamp: Long = 0
)