package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EditMessage @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        conversationID: String,
        messageId: String,
        newContent: String
    ): Flow<Response<Boolean>> {
        return messageRepository.editMessage(conversationID, messageId, newContent)
    }
}