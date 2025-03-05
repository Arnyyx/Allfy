package com.arny.allfy.domain.model

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: Message? = null,
    val unreadCount: Map<String, Int> = emptyMap(),
    val timestamp: Long = 0,
    val createdAt: Long = 0
)