package com.arny.allfy.domain.usecase.conversation

import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetConversationsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository
) {
    operator fun invoke() = conversationRepository.getConversations()
}