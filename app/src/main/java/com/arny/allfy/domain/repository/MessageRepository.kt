package com.arny.allfy.domain.repository

import android.net.Uri
import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.Message
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun loadConversations(userId: String): Flow<Response<List<Conversation>>>
    fun sendMessage(conversationID: String, message: Message): Flow<Response<Boolean>>
    fun sendImages(conversationID: String, imageUris: List<Uri>): Flow<Response<List<String>>>
    fun getMessages(conversationID: String): Flow<List<Message>>
    suspend fun markMessageAsRead(
        conversationId: String,
        userId: String,
        messageId: String
    ): Result<Unit>

    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit>
    fun initializeConversation(userIds: List<String>): Flow<Response<Boolean>>
    fun sendVoiceMessage(conversationID: String, audioUri: Uri): Flow<Response<String>>

}