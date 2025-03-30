package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.repository.MessageRepository
import javax.inject.Inject

class InitializeConversation @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(
        userIds: List<String>
    ) = messageRepository.initializeConversation(userIds)
}