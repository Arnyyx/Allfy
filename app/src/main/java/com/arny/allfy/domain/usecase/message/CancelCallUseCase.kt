package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CancelCallUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        callId: String,
    ): Flow<Response<Boolean>> {
        return messageRepository.cancelCall(conversationId, callId)
    }
}