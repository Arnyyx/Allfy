package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.model.MessageType
import com.arny.allfy.domain.usecase.message.*
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.WebRTCCallManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getMessagesByConversationIdUseCase: GetMessagesByConversationIdUseCase,
    private val markMessageAsReadUseCase: MarkMessageAsReadUseCase,
    private val getOrCreateConversationUseCase: GetOrCreateConversationUseCase,
    private val sendImagesUseCase: SendImagesUseCase,
    private val sendVoiceMessageUseCase: SendVoiceMessageUseCase,
    private val loadConversationsUseCase: LoadConversationsUseCase
) : ViewModel() {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    fun onMessageInputChanged(input: String) {
        _messageInput.value = input
    }

    private val _sendMessageState = MutableStateFlow<Response<Boolean>>(Response.Success(false))
    val sendMessageState: StateFlow<Response<Boolean>> = _sendMessageState.asStateFlow()

    fun sendMessage(conversationId: String, message: Message) {
        viewModelScope.launch {
            _sendMessageState.value = Response.Loading
            val messageWithSender = message.copy(senderId = currentUserId)
            sendMessageUseCase(conversationId, messageWithSender).collect { response ->
                _sendMessageState.value = response
                if (response is Response.Success) {
                    _messageInput.value = ""
                }
            }
        }
    }

    fun sendImages(conversationId: String, imageUris: List<Uri>) {
        viewModelScope.launch {
            _sendMessageState.value = Response.Loading
            sendImagesUseCase(conversationId, imageUris).collect { response ->
                _sendMessageState.value = when (response) {
                    is Response.Success -> Response.Success(true)
                    is Response.Error -> Response.Error(response.message)
                    Response.Loading -> Response.Loading
                }
            }
        }
    }

    private val _conversationState = MutableStateFlow<Response<Conversation>>(Response.Loading)
    val conversationState: StateFlow<Response<Conversation>> = _conversationState.asStateFlow()

    fun initializeChat(currentUserId: String, recipientId: String) {
        viewModelScope.launch {
            getOrCreateConversationUseCase(currentUserId, recipientId).collect { response ->
                _conversationState.value = response
                if (response is Response.Success) {
                    loadMessages(response.data.id)
                }
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

    fun markMessageAsRead(conversationId: String, messageId: String) {
        viewModelScope.launch {
            markMessageAsReadUseCase(conversationId, currentUserId, messageId)
        }
    }

    fun clear() {
        _messageInput.value = ""
        _sendMessageState.value = Response.Success(false)
        _conversationState.value = Response.Loading
        _messages.value = emptyList()
        _loadConversationsState.value = Response.Loading
    }

    private val _loadConversationsState =
        MutableStateFlow<Response<List<Conversation>>>(Response.Loading)
    val loadConversationsState: StateFlow<Response<List<Conversation>>> =
        _loadConversationsState.asStateFlow()

    fun sendVoiceMessage(conversationId: String, audioUri: Uri) {
        viewModelScope.launch {
            _sendMessageState.value = Response.Loading
            sendVoiceMessageUseCase(conversationId, audioUri).collect { response ->
                _sendMessageState.value = when (response) {
                    is Response.Success -> Response.Success(true)
                    is Response.Error -> Response.Error(response.message)
                    Response.Loading -> Response.Loading
                }
            }
        }
    }

    fun loadConversations(userId: String) {
        viewModelScope.launch {
            loadConversationsUseCase(userId).collect { response ->
                _loadConversationsState.value = response
            }
        }
    }

}



