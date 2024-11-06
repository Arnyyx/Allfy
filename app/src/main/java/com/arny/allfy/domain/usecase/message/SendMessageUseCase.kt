package com.arny.allfy.domain.usecase.message

import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.model.MessageType
import com.arny.allfy.domain.repository.MessageRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        receiverId: String,
        content: String,
        type: MessageType = MessageType.TEXT
    ): Result<Unit> {
        val senderId = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Current user ID not found")
        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            type = type
        )
        return messageRepository.sendMessage(message)
    }
}
