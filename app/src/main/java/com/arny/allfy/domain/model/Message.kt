package com.arny.allfy.domain.model

data class Message(
    val id: String = "",
    val senderId: String = "",
    val content: String = "",
    val originalContent: String? = null,
    val timestamp: Long = System.currentTimeMillis(), // Thời gian gửi tin nhắn
    val editedTimestamp: Long? = null, // Thời gian chỉnh sửa
    var type: MessageType = MessageType.TEXT
)

enum class MessageType {
    TEXT, IMAGE, VIDEO, FILE, VOICE, VOICE_CALL, VIDEO_CALL
}