package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MarkMessageAsRead @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        currentUserId: String,
        messageId: String
    ): Flow<Response<Boolean>> {
        return messageRepository.markMessageAsRead(conversationId, currentUserId, messageId)
    }
}