package com.arny.allfy.presentation.state

import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.Message
import com.arny.allfy.utils.Response

data class ChatState(
    val messages: List<Message> = emptyList(),
    val messageInput: String = "",

    // Chat actions states
    val loadConversationsState: Response<List<Conversation>> = Response.Idle,
    val sendMessageState: Response<Boolean> = Response.Idle,
    val sendImagesState: Response<Boolean> = Response.Idle,
    val sendVoiceMessageState: Response<Boolean> = Response.Idle,
    val initializeConversationState: Response<Boolean> = Response.Idle
)