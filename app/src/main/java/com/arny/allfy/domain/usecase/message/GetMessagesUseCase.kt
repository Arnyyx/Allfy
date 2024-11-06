package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(
        senderId: String,
        receiverId: String
    ): Flow<List<Message>> {
        return messageRepository.getMessages(senderId, receiverId)
    }
}
