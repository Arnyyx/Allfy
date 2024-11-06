package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.repository.MessageRepository
import javax.inject.Inject

class MarkMessageAsReadUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(messageId: String): Result<Unit> {
        return messageRepository.markMessageAsRead(messageId)
    }
}