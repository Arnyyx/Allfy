package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessages @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(conversationID: String): Flow<List<Message>> {
        return messageRepository.getMessages(conversationID)
    }
}
