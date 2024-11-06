package com.arny.allfy.domain.model

data class Conversation(
    val id: String,
    val participantId: String,
    val participantName: String,
    val participantAvatar: String,
    val lastMessage: Message?,
    val unreadCount: Int,
    val timestamp: Long
)