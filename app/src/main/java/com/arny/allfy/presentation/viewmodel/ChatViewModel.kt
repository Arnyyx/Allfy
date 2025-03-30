package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.usecase.message.*
import com.arny.allfy.utils.Response
import com.google.firebase.auth.FirebaseAuth
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
                when (response) {
                    is Response.Loading -> _chatState.update { it.copy(isLoadingConversations = true) }
                    is Response.Success -> _chatState.update {
                        it.copy(
                            isLoadingConversations = false,
                            conversations = response.data
                        )
                    }

                    is Response.Error -> _chatState.update {
                        it.copy(
                            isLoadingConversations = false,
                            conversationError = response.message
                        )
                    }

                }
            }
        }
    }


    fun onMessageInputChanged(input: String) {
        _chatState.update { it.copy(messageInput = input) }
    }

    fun sendMessage(conversationId: String, message: Message) {
        viewModelScope.launch {
            messageUseCases.sendMessage(conversationId, message).collect { response ->
                when (response) {
                    is Response.Loading -> _chatState.update { it.copy(isSendingMessage = true) }
                    is Response.Success -> {
                        _chatState.update {
                            it.copy(
                                isSendingMessage = false,
                                messageInput = ""
                            )
                        }
                    }

                    is Response.Error -> _chatState.update {
                        it.copy(
                            isSendingMessage = false,
                            sendMessageError = response.message
                        )
                    }
                }
            }
        }
    }

    fun sendImages(conversationId: String, imageUris: List<Uri>) {
        viewModelScope.launch {
            messageUseCases.sendImages(conversationId, imageUris).collect { response ->
                when (response) {
                    is Response.Loading -> _chatState.update { it.copy(isSendingMessage = true) }
                    is Response.Success -> {
                        _chatState.update {
                            it.copy(isSendingMessage = false)
                        }
                    }

                    is Response.Error -> _chatState.update {
                        it.copy(
                            isSendingMessage = false,
                            sendMessageError = response.message
                        )
                    }
                }
            }
        }
    }

    fun initializeConversation(userIds: List<String>) {
        viewModelScope.launch {
            messageUseCases.initializeConversation(userIds).collect { response ->
                when (response) {
                    is Response.Loading -> _chatState.update { it.copy(isInitializingConversation = true) }

                    is Response.Success -> {
                        _chatState.update { it.copy(isInitializingConversation = false) }
                    }

                    is Response.Error -> _chatState.update {
                        it.copy(
                            isInitializingConversation = false,
                            initializeConversationError = response.message
                        )
                    }
                }
            }
        }
    }

    fun sendVoiceMessage(conversationId: String, audioUri: Uri) {
        viewModelScope.launch {
            messageUseCases.sendVoiceMessage(conversationId, audioUri).collect { response ->
                when (response) {
                    is Response.Loading -> _chatState.update { it.copy(isSendingMessage = true) }
                    is Response.Success -> {
                        _chatState.update {
                            it.copy(isSendingMessage = false)
                        }
                    }

                    is Response.Error -> _chatState.update {
                        it.copy(
                            isSendingMessage = false,
                            sendMessageError = response.message
                        )
                    }
                }
            }
        }
    }

    fun clearChatState() {
        _chatState.value = ChatState()
    }
}

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoadingConversations: Boolean = false,
    val conversations: List<Conversation> = emptyList(),
    val conversationError: String? = null,

    val isSendingMessage: Boolean = false,
    val sendMessageError: String? = null,
    val messageInput: String = "",

    val isInitializingConversation: Boolean = false,
    val initializeConversationError: String? = null
)
