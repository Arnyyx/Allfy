package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.repository.MessageRepository
import javax.inject.Inject

class GetOrCreateConversationUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(
        currentUserId: String,
        recipientId: String
    ) = messageRepository.getOrCreateConversation(currentUserId, recipientId)
}