package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendCallInvitationUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(callerId: String, calleeId: String):Flow<Response<String>>  {
        return messageRepository.sendCallInvitation(callerId, calleeId)
    }
}