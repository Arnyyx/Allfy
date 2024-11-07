package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesByConversationIdUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(
        conversationID: String
    ): Flow<List<Message>> {
        return messageRepository.getMessagesByConversationId(conversationID)
    }
}
