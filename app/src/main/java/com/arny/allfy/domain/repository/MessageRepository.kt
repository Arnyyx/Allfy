package com.arny.allfy.domain.repository

import com.arny.allfy.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun sendMessage(message: Message): Result<Unit>
    fun getMessages(senderId: String, receiverId: String): Flow<List<Message>>
    suspend fun markMessageAsRead(messageId: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
}