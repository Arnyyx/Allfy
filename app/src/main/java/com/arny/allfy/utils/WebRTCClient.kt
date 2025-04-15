package com.arny.allfy.utils

import android.content.Context
import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await
import org.webrtc.*
import org.webrtc.PeerConnection.*
import java.util.concurrent.Executors
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.DefaultVideoDecoderFactory

enum class CallStatus {
    PENDING, CONNECTING, CONNECTED, ENDED, ERROR
}

data class CallState(
    val status: CallStatus,
    val errorMessage: String? = null
)

class WebRTCClient(
    private val context: Context,
    private val eglBaseContext: EglBase.Context?,
    private val conversationId: String,
    private val isCaller: Boolean,
    private val callerId: String,
    private val onVideoTrackReceived: (VideoTrack) -> Unit,
    private val onStateChange: (CallState) -> Unit
) {
    private val TAG = "WebRTCClient"
    private val database = FirebaseDatabase.getInstance().reference
    private val callRef = database.child("conversations").child(conversationId).child("calls")
    private val executor = Executors.newSingleThreadExecutor()
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var audioSource: AudioSource? = null
    private var remoteSdp: SessionDescription? = null
    private var remoteIceCandidates = mutableListOf<IceCandidate>()
    private var isCameraInitialized = false
    private var isDisposed = false
    private var sdpListener: ValueEventListener? = null
    private var iceListener: ValueEventListener? = null
    private var statusListener: ValueEventListener? = null

    private val sdpConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
    }

    init {
        try {
            initializeWebRTC()
            setupFirebaseSignaling()
            onStateChange(CallState(CallStatus.PENDING))
        } catch (e: Exception) {
            handleError("Initialization failed: ${e.message}", CallStatus.ERROR)
        }
    }

    private fun initializeWebRTC() {
        try {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                    .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                    .createInitializationOptions()
            )

            val options = PeerConnectionFactory.Options()
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBaseContext, true, true))
                .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBaseContext))
                .createPeerConnectionFactory()

            val iceServers =
                listOf(
                    IceServer.builder("turn:global.relay.metered.ca:80")
                        .setUsername("b297fb6c8b566cfecc2c34bf")
                        .setPassword("Ztt+3/fglCFMbG+H")
                        .createIceServer(),
                    IceServer.builder("turn:global.relay.metered.ca:80?transport=tcp")
                        .setUsername("b297fb6c8b566cfecc2c34bf")
                        .setPassword("Ztt+3/fglCFMbG+H")
                        .createIceServer(),
                    IceServer.builder("turn:global.relay.metered.ca:443")
                        .setUsername("b297fb6c8b566cfecc2c34bf")
                        .setPassword("Ztt+3/fglCFMbG+H")
                        .createIceServer(),
                    IceServer.builder("turns:global.relay.metered.ca:443?transport=tcp")
                        .setUsername("b297fb6c8b566cfecc2c34bf")
                        .setPassword("Ztt+3/fglCFMbG+H")
                        .createIceServer(),
                    IceServer.builder("stun:stun.relay.metered.ca:80").createIceServer(),
                    IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
                )
            val rtcConfig = RTCConfiguration(iceServers).apply {
                sdpSemantics = SdpSemantics.UNIFIED_PLAN
                iceTransportsType = IceTransportsType.ALL
            }
            peerConnection = peerConnectionFactory?.createPeerConnection(
                rtcConfig,
                object : Observer {
                    override fun onSignalingChange(state: SignalingState?) {
                        Log.d(TAG, "SignalingState: $state")
                    }

                    override fun onIceCandidate(candidate: IceCandidate) {
                        if (isDisposed) return
                        executor.execute {
                            callRef.child("iceCandidates").push().setValue(
                                mapOf(
                                    "sdpMid" to candidate.sdpMid,
                                    "sdpMLineIndex" to candidate.sdpMLineIndex,
                                    "sdp" to candidate.sdp
                                )
                            ).addOnFailureListener {
                                handleError("Failed to send ICE candidate: ${it.message}")
                            }
                        }
                    }

                    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}

                    override fun onIceConnectionChange(state: IceConnectionState?) {
                        Log.d(TAG, "IceConnectionState: $state")
                        if (isDisposed) return
                        when (state) {
                            IceConnectionState.CONNECTED -> onStateChange(CallState(CallStatus.CONNECTED))
                            IceConnectionState.DISCONNECTED -> handleError("Network disconnected")
                            IceConnectionState.FAILED -> handleError("ICE connection failed")
                            else -> {}
                        }
                    }

                    override fun onIceGatheringChange(state: IceGatheringState?) {
                        Log.d(TAG, "IceGatheringState: $state")
                    }

                    override fun onConnectionChange(state: PeerConnectionState?) {
                        Log.d(TAG, "ConnectionState: $state")
                    }

                    override fun onIceConnectionReceivingChange(p0: Boolean) {}

                    override fun onAddStream(stream: MediaStream?) {}

                    override fun onRemoveStream(stream: MediaStream?) {}

                    override fun onDataChannel(channel: DataChannel?) {}

                    override fun onRenegotiationNeeded() {}

                    override fun onAddTrack(
                        receiver: RtpReceiver?,
                        streams: Array<out MediaStream>?
                    ) {
                        if (isDisposed) return
                        receiver?.track()?.let { track ->
                            when (track) {
                                is VideoTrack -> {
                                    onVideoTrackReceived(track)
                                }

                                is AudioTrack -> Log.d(
                                    TAG,
                                    "Remote audio track received: ${track.id()}"
                                )

                                else -> {}
                            }
                        }
                    }
                }
            ) ?: throw IllegalStateException("Failed to create peer connection")

            if (isCaller) {
                initializeCameraAndAudio()
            }
        } catch (e: Exception) {
            handleError("WebRTC initialization failed: ${e.message}", CallStatus.ERROR)
            throw e
        }
    }


    private fun initializeCameraAndAudio() {
        if (isCameraInitialized || isDisposed) return
        try {
            // Video
            videoCapturer = createVideoCapturer()
            if (videoCapturer == null) {
                throw IllegalStateException("No camera found")
            }
            val videoSource = peerConnectionFactory?.createVideoSource(videoCapturer!!.isScreencast)
                ?: throw IllegalStateException("Failed to create video source")
            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
            videoCapturer?.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
            videoCapturer?.startCapture(1920, 720, 60)
            localVideoTrack = peerConnectionFactory?.createVideoTrack("100", videoSource)
                ?: throw IllegalStateException("Failed to create video track")
            localVideoTrack?.setEnabled(true)

            // Audio
            audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
                ?: throw IllegalStateException("Failed to create audio source")
            localAudioTrack = peerConnectionFactory?.createAudioTrack("101", audioSource)
                ?: throw IllegalStateException("Failed to create audio track")
            localAudioTrack?.setEnabled(true)
            Log.d(TAG, "Local audio track created and enabled: ${localAudioTrack?.id()}")

            if (isCaller) {
                val streamId = "stream1"
                if (localVideoTrack != null) {
                    peerConnection?.addTrack(localVideoTrack!!, listOf(streamId))

                }
                if (localAudioTrack != null) {
                    peerConnection?.addTrack(localAudioTrack!!, listOf(streamId))
                }
            }

            isCameraInitialized = true
        } catch (e: Exception) {
            handleError("Camera or audio initialization failed: ${e.message}", CallStatus.ERROR)
            throw e
        }
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceName = enumerator.deviceNames
            .firstOrNull { name -> enumerator.isBackFacing(name) }
        return deviceName?.let { enumerator.createCapturer(it, null) }
    }

    private fun setupFirebaseSignaling() {
        sdpListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isDisposed) return
                try {
                    val sdpData =
                        snapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                    if (sdpData != null) {
                        val type = sdpData["type"] as? String ?: return
                        val description = sdpData["description"] as? String ?: return
                        remoteSdp = SessionDescription(
                            if (type == "offer") SessionDescription.Type.OFFER
                            else SessionDescription.Type.ANSWER,
                            description
                        )
                        if (!isCaller && type == "offer") {
                            onStateChange(CallState(CallStatus.PENDING))
                        } else if (isCaller && type == "answer") {
                            handleRemoteSdp()
                        }
                    }
                } catch (e: Exception) {
                    handleError("Failed to process SDP: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isDisposed) return
                handleError("Firebase SDP error: ${error.message}")
            }
        }
        callRef.child("sdp").addValueEventListener(sdpListener!!)

        iceListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isDisposed) return
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
                            remoteIceCandidates.add(candidate)
                            if (peerConnection?.remoteDescription != null && !isDisposed) {
                                peerConnection?.addIceCandidate(candidate)
                            }
                        }
                    }
                } catch (e: Exception) {
                    handleError("Failed to process ICE candidate: ${e.message}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isDisposed) return
                handleError("Firebase ICE error: ${error.message}")
            }
        }
        callRef.child("iceCandidates").addValueEventListener(iceListener!!)

        statusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isDisposed) return
                val status = snapshot.getValue(String::class.java)
                when (status) {
                    "ENDED" -> {
                        onStateChange(CallState(CallStatus.ENDED))
                        cleanup()
                    }

                    "ERROR" -> {
                        callRef.child("error").get().addOnSuccessListener { errorSnapshot ->
                            val errorMsg =
                                errorSnapshot.getValue(String::class.java) ?: "Unknown error"
                            onStateChange(CallState(CallStatus.ERROR, errorMsg))
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isDisposed) return
                handleError("Firebase status error: ${error.message}")
            }
        }
        callRef.child("status").addValueEventListener(statusListener!!)
    }

    private fun handleRemoteSdp() {
        if (isDisposed || peerConnection == null) return
        remoteSdp?.let { sdp ->
            executor.execute {
                try {
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}

                        override fun onSetSuccess() {
                            if (isDisposed) return

                            onStateChange(CallState(CallStatus.CONNECTING))
                            if (!isCaller && sdp.type == SessionDescription.Type.OFFER) {
                                if (localVideoTrack != null) {
                                    peerConnection?.addTrack(
                                        localVideoTrack!!,
                                        listOf("stream1")
                                    )

                                }
                                if (localAudioTrack != null) {
                                    peerConnection?.addTrack(localAudioTrack!!, listOf("stream1"))

                                }
                                peerConnection?.createAnswer(object : SdpObserver {
                                    override fun onCreateSuccess(answer: SessionDescription?) {
                                        if (isDisposed) return

                                        peerConnection?.setLocalDescription(object : SdpObserver {
                                            override fun onCreateSuccess(p0: SessionDescription?) {}
                                            override fun onSetSuccess() {
                                                if (isDisposed) return
                                                callRef.child("sdp").setValue(
                                                    mapOf(
                                                        "type" to "answer",
                                                        "description" to answer?.description
                                                    )
                                                ).addOnFailureListener {
                                                    handleError("Failed to send answer: ${it.message}")
                                                }
                                            }

                                            override fun onCreateFailure(p0: String?) {}
                                            override fun onSetFailure(p0: String?) {
                                                if (isDisposed) return
                                                handleError("Set local description failed: $p0")
                                            }
                                        }, answer)
                                    }

                                    override fun onSetSuccess() {}
                                    override fun onCreateFailure(p0: String?) {}
                                    override fun onSetFailure(p0: String?) {
                                        if (isDisposed) return
                                        handleError("Create answer failed: $p0")
                                    }
                                }, sdpConstraints)
                            }
                            remoteIceCandidates.forEach { candidate ->
                                if (!isDisposed) peerConnection?.addIceCandidate(candidate)
                            }
                        }

                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {
                            if (isDisposed) return
                            handleError("Set remote description failed: $p0")
                        }
                    }, sdp)
                } catch (e: Exception) {
                    handleError("Failed to handle remote SDP: ${e.message}")
                }
            }
        }
    }

    fun startCall() {
        if (!isCaller || isDisposed) return
        executor.execute {
            try {
                peerConnection?.createOffer(object : SdpObserver {
                    override fun onCreateSuccess(offer: SessionDescription?) {
                        if (isDisposed) return
                        peerConnection?.setLocalDescription(object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                if (isDisposed) return
                                callRef.child("sdp").setValue(
                                    mapOf(
                                        "type" to "offer",
                                        "description" to offer?.description
                                    )
                                ).addOnSuccessListener {
                                    callRef.child("status").setValue("PENDING")
                                    callRef.child("callerId").setValue(callerId)
                                }.addOnFailureListener {
                                    handleError("Failed to send offer: ${it.message}")
                                }
                            }

                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(p0: String?) {
                                if (isDisposed) return
                                handleError("Set local description failed: $p0")
                            }
                        }, offer)
                    }

                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {
                        if (isDisposed) return
                        handleError("Create offer failed: $p0")
                    }
                }, sdpConstraints)
            } catch (e: Exception) {
                handleError("Failed to start call: ${e.message}")
            }
        }
    }

    fun acceptCall() {
        if (isCaller || isDisposed) return
        try {
            initializeCameraAndAudio()
            handleRemoteSdp()
        } catch (e: Exception) {
            handleError(
                "Failed to initialize camera/audio on accept: ${e.message}",
                CallStatus.ERROR
            )
        }
    }


    fun rejectCall() {
        if (isDisposed) return
        executor.execute {
            callRef.child("status").setValue("ENDED")
            onStateChange(CallState(CallStatus.ENDED))
            cleanup()
        }
    }

    fun endCall() {
        if (isDisposed) return
        executor.execute {
            callRef.child("status").setValue("ENDED")
            onStateChange(CallState(CallStatus.ENDED))
            cleanup()
        }
    }

    fun getLocalVideoTrack(): VideoTrack? {
        return localVideoTrack
    }

    private fun handleError(message: String, status: CallStatus = CallStatus.ERROR) {
        if (isDisposed) return
        Log.e(TAG, message)
        executor.execute {
            callRef.child("status").setValue("ERROR")
            callRef.child("error").setValue(message)
            onStateChange(CallState(status, message))
        }
    }

    fun cleanup() {
        if (isDisposed) return
        try {
            executor.execute {
                videoCapturer?.stopCapture()
                videoCapturer?.dispose()
                audioSource?.dispose()
                peerConnection?.close()
                peerConnection?.dispose()
                peerConnectionFactory?.dispose()
                localVideoTrack?.dispose()
                localAudioTrack?.dispose()
                videoCapturer = null
                audioSource = null
                localVideoTrack = null
                localAudioTrack = null
                isCameraInitialized = false

                sdpListener?.let { callRef.child("sdp").removeEventListener(it) }
                iceListener?.let { callRef.child("iceCandidates").removeEventListener(it) }
                statusListener?.let { callRef.child("status").removeEventListener(it) }
                sdpListener = null
                iceListener = null
                statusListener = null

                callRef.removeValue()
                isDisposed = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed: ${e.message}")
        }
    }
}