package com.arny.allfy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.usecase.message.GetMessagesByConversationIdUseCase
import com.arny.allfy.domain.usecase.message.GetOrCreateConversationUseCase
import com.arny.allfy.domain.usecase.message.LoadConversationsUseCase
import com.arny.allfy.domain.usecase.message.MarkMessageAsReadUseCase
import com.arny.allfy.domain.usecase.message.SendMessageUseCase
import com.arny.allfy.utils.Response
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
    private val loadConversationsUseCase: LoadConversationsUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val getMessagesByConversationIdUseCase: GetMessagesByConversationIdUseCase,
    private val markMessageAsReadUseCase: MarkMessageAsReadUseCase,
    private val getOrCreateConversationUseCase: GetOrCreateConversationUseCase
) : ViewModel() {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        ?: ""

    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    fun onMessageInputChanged(input: String) {
        _messageInput.value = input
    }

    private val _sendMessageSate = MutableStateFlow<Response<Boolean>>(Response.Loading)
    val sendMessageState: StateFlow<Response<Boolean>> = _sendMessageSate.asStateFlow()

    fun sendMessage(conversationID: String, message: Message) {
        viewModelScope.launch {
            sendMessageUseCase(
                conversationID = conversationID,
                message
            ).collect { response ->
                _sendMessageSate.value = response
            }
        }
    }

    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            markMessageAsReadUseCase(messageId)
        }
    }

    private val _conversationState = MutableStateFlow<Response<Conversation>>(Response.Loading)
    val conversationState = _conversationState.asStateFlow()

    fun initializeChat(currentUserId: String, recipientId: String) {
        viewModelScope.launch {
            _conversationState.value = Response.Loading
            try {
                getOrCreateConversationUseCase(currentUserId, recipientId)
                    .collect { conversationId ->
                        _conversationState.value = conversationId
                        if (conversationId is Response.Success)
                            loadMessages(conversationId.data.id)
                    }
            } catch (e: Exception) {
                _conversationState.value = Response.Error(e.message ?: "Unknown error")
            }
        }
    }

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            getMessagesByConversationIdUseCase(conversationId).collect { messages ->
                _messages.value = messages
            }
        }
    }

    private val _loadConversationsState =
        MutableStateFlow<Response<List<Conversation>>>(Response.Loading)
    val loadConversationsState: StateFlow<Response<List<Conversation>>> =
        _loadConversationsState.asStateFlow()

    fun loadConversations(userId: String) {
        viewModelScope.launch {
            loadConversationsUseCase(userId).collect { response ->
                _loadConversationsState.value = response
            }
        }
    }

    fun resetConversationState() {
        _conversationState.value = Response.Loading
    }
}
