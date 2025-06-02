package com.arny.allfy.presentation.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.repository.CallRepository
import com.arny.allfy.utils.CallState
import com.arny.allfy.utils.CallStatus
import com.arny.allfy.utils.WebRTCClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.VideoTrack
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    application: Application,
    private val callRepository: CallRepository
) : AndroidViewModel(application) {

    private val _callState = MutableStateFlow(CallState(CallStatus.PENDING))
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private lateinit var webRTCClient: WebRTCClient
    private lateinit var eglBase: EglBase

    fun initialize(conversationId: String, isCaller: Boolean, callerId: String) {
        viewModelScope.launch {
            eglBase = EglBase.create()
            webRTCClient = WebRTCClient(
                context = getApplication(),
                eglBaseContext = eglBase.eglBaseContext,
                conversationId = conversationId,
                isCaller = isCaller,
                callerId = callerId,
                callRepository = callRepository,
                onVideoTrackReceived = { track ->
                    _remoteVideoTrack.value = track
                },
                onStateChange = { state ->
                    _callState.value = state
                }
            )
            if (isCaller && hasPermissions()) {
                _localVideoTrack.value = webRTCClient.getLocalVideoTrack()
                webRTCClient.startCall()
            }
        }
    }

    fun startCall() {
        if (hasPermissions()) {
            viewModelScope.launch {
                _localVideoTrack.value = webRTCClient.getLocalVideoTrack()
                webRTCClient.startCall()
            }
        }
    }

    fun acceptCall() {
        viewModelScope.launch {
            webRTCClient.acceptCall()
            _localVideoTrack.value = webRTCClient.getLocalVideoTrack()
        }
    }

    fun rejectCall() {
        viewModelScope.launch {
            webRTCClient.rejectCall()
        }
    }

    fun endCall() {
        viewModelScope.launch {
            webRTCClient.endCall()
        }
    }

    fun hasPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    getApplication(),
                    android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCleared() {
        super.onCleared()
        webRTCClient.cleanup()
        eglBase.release()
    }
}