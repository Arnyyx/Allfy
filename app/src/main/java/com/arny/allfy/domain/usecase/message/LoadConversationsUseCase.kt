package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoadConversationsUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(
        userId: String
    ): Flow<Response<List<Conversation>>> {
        return messageRepository.loadConversations(userId)
    }
}
