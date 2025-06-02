package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.usecase.message.MessageUseCases
import com.arny.allfy.presentation.state.ChatState
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.isSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageUseCases: MessageUseCases
) : ViewModel() {

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            messageUseCases.getMessages(conversationId).collect { messages ->
                _chatState.update { it.copy(messages = messages) }
            }
        }
    }

    fun loadConversations(userId: String) {
        viewModelScope.launch {
            messageUseCases.loadConversations(userId).collect { response ->
                _chatState.update { it.copy(loadConversationsState = response) }
            }
        }
    }

    fun onMessageInputChanged(input: String) {
        _chatState.update { it.copy(messageInput = input) }
    }

    fun sendMessage(conversationId: String, message: Message) {
        viewModelScope.launch {
            messageUseCases.sendMessage(conversationId, message).collect { response ->
                _chatState.update { it.copy(sendMessageState = response) }
                if (response is Response.Success) {
                    _chatState.update { it.copy(messageInput = "") }
                }
            }
        }
    }

    fun sendImages(conversationId: String, imageUris: List<Uri>) {
        viewModelScope.launch {
            messageUseCases.sendImages(conversationId, imageUris).collect { response ->
                _chatState.update { it.copy(sendImagesState = response) }
            }
        }
    }

    fun sendVoiceMessage(conversationId: String, audioUri: Uri) {
        viewModelScope.launch {
            messageUseCases.sendVoiceMessage(conversationId, audioUri).collect { response ->
                _chatState.update { it.copy(sendVoiceMessageState = response) }
            }
        }
    }

    fun initializeConversation(userIds: List<String>) {
        viewModelScope.launch {
            messageUseCases.initializeConversation(userIds).collect { response ->
                _chatState.update { it.copy(initializeConversationState = response) }
            }
        }
    }

    fun deleteMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            _chatState.update { it.copy(deleteMessageState = Response.Loading) }
            messageUseCases.deleteMessage(conversationId, messageId).collect { response ->
                _chatState.update { it.copy(deleteMessageState = response) }
            }
        }
    }

    // Reset Functions
    fun resetLoadConversationsState() {
        _chatState.update { it.copy(loadConversationsState = Response.Idle) }
    }

    fun resetSendMessageState() {
        _chatState.update { it.copy(sendMessageState = Response.Idle) }
    }

    fun resetSendImagesState() {
        _chatState.update { it.copy(sendImagesState = Response.Idle) }
    }

    fun resetSendVoiceMessageState() {
        _chatState.update { it.copy(sendVoiceMessageState = Response.Idle) }
    }

    fun resetInitializeConversationState() {
        _chatState.update { it.copy(initializeConversationState = Response.Idle) }
    }

    fun resetDeleteMessageState() {
        _chatState.update { it.copy(deleteMessageState = Response.Idle) }
    }

    fun clearChatState() {
        _chatState.value = ChatState()
    }
}