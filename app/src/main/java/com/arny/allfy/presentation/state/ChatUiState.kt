package com.arny.allfy.presentation.state

import com.arny.allfy.domain.model.Message

sealed interface ChatUiState {
    object Initial : ChatUiState
    object SendingMessage : ChatUiState
    object MessageSent : ChatUiState
    data class Success(val messages: List<Message>) : ChatUiState
    data class Error(val message: String) : ChatUiState
}