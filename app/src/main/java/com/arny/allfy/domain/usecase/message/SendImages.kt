package com.arny.allfy.domain.usecase.message

import android.net.Uri
import com.arny.allfy.domain.repository.MessageRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendImages @Inject constructor(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        conversationID: String,
        imageUris: List<Uri>
    ): Flow<Response<List<String>>> {
        return messageRepository.sendImages(conversationID, imageUris)
    }
}