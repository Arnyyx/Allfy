package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.repository.MessageRepository
import javax.inject.Inject

class MarkMessageAsRead @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        currentUserId: String,
        messageId: String
    ): Result<Unit> {
        return messageRepository.markMessageAsRead(conversationId, currentUserId, messageId)
    }
}