package com.arny.allfy.data.model

data class ConversationEntity(
    val id: String = "",
    val participantId: String = "",
    val participantName: String = "",
    val participantAvatar: String = "",
    val lastMessageId: String = "",
    val unreadCount: Int = 0,
    val timestamp: Long = 0
)