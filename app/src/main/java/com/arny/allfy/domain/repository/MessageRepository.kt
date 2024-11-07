package com.arny.allfy.domain.repository

import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.Message
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun loadConversations(userId: String): Flow<Response<List<Conversation>>>
    fun sendMessage(conversationID: String, message: Message): Flow<Response<Boolean>>
    fun getMessagesByConversationId(conversationID: String): Flow<List<Message>>
    suspend fun markMessageAsRead(messageId: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    fun getOrCreateConversation(
        currentUserId: String,
        recipientId: String
    ): Flow<Response<Conversation>>
}