package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
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
    private val loadConversationsUseCase: LoadConversationsUseCase,

    private val sendCallInvitationUseCase: SendCallInvitationUseCase,
    private val cancelCallUseCase: CancelCallUseCase,
    private val acceptCallUseCase: AcceptCallUseCase,
    private val rejectCallUseCase: RejectCallUseCase,
    private val endCallUseCase: EndCallUseCase,
    private val listenCallStateUseCase: ListenCallStateUseCase
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
                    listenCallState(response.data.id)
                }
            }
        }
    }

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private fun loadMessages(conversationId: String) {
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
        _callState.value = "idle"
        currentCallId = null
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

    private var currentCallId: String? = null
    private val _callState = MutableStateFlow("idle")
    val callState: StateFlow<String> = _callState.asStateFlow()


    private fun listenCallState(conversationId: String) {
        viewModelScope.launch {
            listenCallStateUseCase(conversationId).collect { state ->
                _callState.value = state
                when (state) {
                    "rejected" -> {
                        currentCallId = null
//                        navHostController.popBackStack()
                    }

                    "idle" -> currentCallId = null
                }
            }
        }
    }

    fun sendCallInvitation(callerId: String, calleeId: String) {
        viewModelScope.launch {
            sendCallInvitationUseCase(callerId, calleeId).collect { response ->
                when (response) {
                    is Response.Success -> {
                        currentCallId = response.data
                        _callState.value = "pending"
//                        navHostController.navigate("call/$callerId/$calleeId")
                    }

                    is Response.Error -> {}
                    else -> {}
                }
            }
        }
    }

    fun cancelCall(conversationId: String) {
        viewModelScope.launch {
            currentCallId?.let { callId ->
                cancelCallUseCase(conversationId, callId).collect { response ->
                    if (response is Response.Success) {
                        currentCallId = null
                        _callState.value = "idle"
                    }
                }
            }
        }
    }

    fun acceptCall(conversationId: String, callId: String) {
        viewModelScope.launch {
            acceptCallUseCase(conversationId, callId).collect { response ->
                if (response is Response.Success) {
                    _callState.value = "accepted"
                    currentCallId = callId
//                    navHostController.navigate("call/${FirebaseAuth.getInstance().currentUser?.uid}/$conversationId.split('_').first()")
                }
            }
        }
    }

    fun rejectCall(conversationId: String, callId: String) {
        viewModelScope.launch {
            rejectCallUseCase(conversationId, callId).collect { response ->
                if (response is Response.Success) {
                    _callState.value = "rejected"
                    currentCallId = null
                }
            }
        }
    }

    fun endCall(conversationId: String, duration: Long) {
        viewModelScope.launch {
            currentCallId?.let { callId ->
                endCallUseCase(conversationId, callId, duration).collect { response ->
                    if (response is Response.Success) {
                        currentCallId = null
                        _callState.value = "idle"
                    }
                }
            }
        }
    }
}



