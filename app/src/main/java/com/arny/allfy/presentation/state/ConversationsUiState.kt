package com.arny.allfy.presentation.state

import com.arny.allfy.domain.model.Conversation

sealed interface ConversationsUiState {
    object Initial : ConversationsUiState
    data class Success(val conversations: List<Conversation>) : ConversationsUiState
    data class Error(val message: String) : ConversationsUiState
}
