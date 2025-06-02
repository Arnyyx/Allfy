package com.arny.allfy.domain.repository

import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

interface CallRepository {
    suspend fun sendSdp(
        conversationId: String,
        type: String,
        description: String
    ): Flow<Response<Boolean>>

    suspend fun listenSdp(
        conversationId: String,
        onSdpReceived: (SessionDescription) -> Unit,
        onError: (String) -> Unit
    ): Flow<Unit>

    suspend fun sendIceCandidate(
        conversationId: String,
        candidate: IceCandidate
    ): Flow<Response<Boolean>>

    suspend fun listenIceCandidates(
        conversationId: String,
        onIceCandidateReceived: (IceCandidate) -> Unit,
        onError: (String) -> Unit
    ): Flow<Unit>

    suspend fun updateCallStatus(
        conversationId: String,
        status: String,
        errorMessage: String? = null
    ): Flow<Response<Boolean>>

    suspend fun listenCallStatus(
        conversationId: String,
        onStatusChanged: (String, String?) -> Unit,
        onError: (String) -> Unit
    ): Flow<Unit>

    suspend fun setCallerId(
        conversationId: String,
        callerId: String
    ): Flow<Response<Boolean>>

    suspend fun cleanupCall(conversationId: String): Flow<Response<Boolean>>
}