package com.arny.allfy.domain.usecase.message

import android.net.Uri
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendVoiceMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    operator fun invoke(
        conversationID: String, audioUri: Uri,
    ): Flow<Response<String>> {
        return messageRepository.sendVoiceMessage(conversationID, audioUri)
    }
}