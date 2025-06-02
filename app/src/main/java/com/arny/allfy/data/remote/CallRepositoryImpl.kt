package com.arny.allfy.data.remote

import com.arny.allfy.utils.Response
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import javax.inject.Inject

class CallRepositoryImpl @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) : com.arny.allfy.domain.repository.CallRepository {

    override suspend fun sendSdp(
        conversationId: String,
        type: String,
        description: String
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val callRef = firebaseDatabase.reference
                .child("conversations")
                .child(conversationId)
                .child("calls")
                .child("sdp")
            callRef.setValue(
                mapOf(
                    "type" to type,
                    "description" to description
                )
            ).await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to send SDP"))
        }
    }

    override suspend fun listenSdp(
        conversationId: String,
        onSdpReceived: (SessionDescription) -> Unit,
        onError: (String) -> Unit
    ): Flow<Unit> = callbackFlow {
        val callRef = firebaseDatabase.reference
            .child("conversations")
            .child(conversationId)
            .child("calls")
            .child("sdp")

        val listener = callRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val sdpData =
                        snapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                    if (sdpData != null) {
                        val type = sdpData["type"] as? String ?: return
                        val description = sdpData["description"] as? String ?: return
                        val sessionDescription = SessionDescription(
                            if (type == "offer") SessionDescription.Type.OFFER
                            else SessionDescription.Type.ANSWER,
                            description
                        )
                        onSdpReceived(sessionDescription)
                    }
                } catch (e: Exception) {
                    onError("Failed to process SDP: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onError("Firebase SDP error: ${error.message}")
            }
        })

        awaitClose {
            callRef.removeEventListener(listener)
        }
    }

    override suspend fun sendIceCandidate(
        conversationId: String,
        candidate: IceCandidate
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val callRef = firebaseDatabase.reference
                .child("conversations")
                .child(conversationId)
                .child("calls")
                .child("iceCandidates")
                .push()
            callRef.setValue(
                mapOf(
                    "sdpMid" to candidate.sdpMid,
                    "sdpMLineIndex" to candidate.sdpMLineIndex,
                    "sdp" to candidate.sdp
                )
            ).await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to send ICE candidate"))
        }
    }

    override suspend fun listenIceCandidates(
        conversationId: String,
        onIceCandidateReceived: (IceCandidate) -> Unit,
        onError: (String) -> Unit
    ): Flow<Unit> = callbackFlow {
        val callRef = firebaseDatabase.reference
            .child("conversations")
            .child(conversationId)
            .child("calls")
            .child("iceCandidates")

        val listener = callRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    for (data in snapshot.children) {
                        val candidateData =
                            data.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                        if (candidateData != null) {
                            val sdpMid = candidateData["sdpMid"] as? String ?: continue
                            val sdpMLineIndex =
                                (candidateData["sdpMLineIndex"] as? Long)?.toInt() ?: continue
                            val sdp = candidateData["sdp"] as? String ?: continue
                            val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                            onIceCandidateReceived(candidate)
                        }
                    }
                } catch (e: Exception) {
                    onError("Failed to process ICE candidate: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onError("Firebase ICE error: ${error.message}")
            }
        })

        awaitClose {
            callRef.removeEventListener(listener)
        }
    }

    override suspend fun updateCallStatus(
        conversationId: String,
        status: String,
        errorMessage: String?
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            val callRef = firebaseDatabase.reference
                .child("conversations")
                .child(conversationId)
                .child("calls")
            val updates = mutableMapOf<String, Any>("status" to status)
            if (errorMessage != null) {
                updates["error"] = errorMessage
            }
            callRef.updateChildren(updates).await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to update call status"))
        }
    }

    override suspend fun listenCallStatus(
        conversationId: String,
        onStatusChanged: (String, String?) -> Unit,
        onError: (String) -> Unit
    ): Flow<Unit> = callbackFlow {
        val callRef = firebaseDatabase.reference
            .child("conversations")
            .child(conversationId)
            .child("calls")
            .child("status")

        val listener = callRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status == "ERROR") {
                    firebaseDatabase.reference
                        .child("conversations")
                        .child(conversationId)
                        .child("calls")
                        .child("error")
                        .get()
                        .addOnSuccessListener { errorSnapshot ->
                            val errorMsg =
                                errorSnapshot.getValue(String::class.java) ?: "Unknown error"
                            onStatusChanged(status, errorMsg)
                        }
                        .addOnFailureListener { onError("Failed to fetch error message") }
                } else {
                    onStatusChanged(status ?: "", null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onError("Firebase status error: ${error.message}")
            }
        })

        awaitClose {
            callRef.removeEventListener(listener)
        }
    }

    override suspend fun setCallerId(
        conversationId: String,
        callerId: String
    ): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            firebaseDatabase.reference
                .child("conversations")
                .child(conversationId)
                .child("calls")
                .child("callerId")
                .setValue(callerId)
                .await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to set caller ID"))
        }
    }

    override suspend fun cleanupCall(conversationId: String): Flow<Response<Boolean>> = flow {
        emit(Response.Loading)
        try {
            firebaseDatabase.reference
                .child("conversations")
                .child(conversationId)
                .child("calls")
                .removeValue()
                .await()
            emit(Response.Success(true))
        } catch (e: Exception) {
            emit(Response.Error(e.message ?: "Failed to cleanup call"))
        }
    }
}