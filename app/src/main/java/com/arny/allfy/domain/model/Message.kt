package com.arny.allfy.domain.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var type: MessageType = MessageType.TEXT
)

enum class MessageType {
    TEXT, IMAGE, VIDEO, FILE
}