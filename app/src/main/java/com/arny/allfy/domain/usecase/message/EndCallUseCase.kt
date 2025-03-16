package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EndCallUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(conversationId: String, callId: String, duration: Long): Flow<Response<Boolean>> {
        return messageRepository.endCall(conversationId, callId, duration)
    }
}