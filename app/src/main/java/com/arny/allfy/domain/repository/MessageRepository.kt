package com.arny.allfy.domain.repository

import android.net.Uri
import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.Message
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun loadConversations(userId: String): Flow<Response<List<Conversation>>>
    suspend fun sendMessage(conversationID: String, message: Message): Flow<Response<Boolean>>
    suspend fun sendImages(
        conversationID: String,
        imageUris: List<Uri>
    ): Flow<Response<Boolean>>

    suspend fun getMessages(conversationID: String): Flow<List<Message>>
    suspend fun markMessageAsRead(
        conversationId: String,
        userId: String,
        messageId: String
    ): Flow<Response<Boolean>>

    suspend fun deleteMessage(conversationId: String, messageId: String): Flow<Response<Boolean>>
    suspend fun initializeConversation(userIds: List<String>): Flow<Response<Boolean>>
    suspend fun sendVoiceMessage(conversationID: String, audioUri: Uri): Flow<Response<Boolean>>
}