package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendMessage @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(
        conversationID: String,
        message: Message,
    ): Flow<Response<Boolean>> {
        return messageRepository.sendMessage(conversationID, message)
    }
}
