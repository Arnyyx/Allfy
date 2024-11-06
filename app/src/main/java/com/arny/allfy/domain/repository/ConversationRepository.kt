package com.arny.allfy.domain.repository

import com.arny.allfy.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    fun getConversations(): Flow<List<Conversation>>

    suspend fun getConversationByParticipant(
        participantId: String
    ): Result<Conversation>

    suspend fun updateConversation(
        conversation: Conversation
    ): Result<Unit>
}