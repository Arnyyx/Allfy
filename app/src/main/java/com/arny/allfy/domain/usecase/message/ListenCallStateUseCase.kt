package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ListenCallStateUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(conversationId: String): Flow<String> {
        return messageRepository.listenCallState(conversationId)
    }
}

