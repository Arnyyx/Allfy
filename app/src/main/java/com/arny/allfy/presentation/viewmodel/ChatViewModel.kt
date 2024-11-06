package com.arny.allfy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.usecase.message.GetMessagesUseCase
import com.arny.allfy.domain.usecase.message.MarkMessageAsReadUseCase
import com.arny.allfy.domain.usecase.message.SendMessageUseCase
import com.arny.allfy.presentation.state.ChatUiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val markMessageAsReadUseCase: MarkMessageAsReadUseCase
) : ViewModel() {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        ?: throw IllegalStateException("Current user ID not found")

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Initial)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    fun onMessageInputChanged(input: String) {
        _messageInput.value = input
    }


    fun sendMessage(receiverId: String, content: String) {
        viewModelScope.launch {
            _uiState.value = ChatUiState.SendingMessage
            sendMessageUseCase(
                receiverId = receiverId,
                content = content
            ).onSuccess {
                _messageInput.value = ""
                _uiState.value = ChatUiState.MessageSent
            }.onFailure { error ->
                _uiState.value = ChatUiState.Error(error.message ?: "Failed to send message")
            }
        }
    }

    fun observeMessages(otherUserId: String) {
        viewModelScope.launch {
            getMessagesUseCase(
                senderId = currentUserId,
                receiverId = otherUserId
            ).catch {
                _messages.value = emptyList()
            }.collect { messages ->
                _messages.value = messages
            }
        }
    }

    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            markMessageAsReadUseCase(messageId)
        }
    }
}
