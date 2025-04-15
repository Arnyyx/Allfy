package com.arny.allfy.presentation.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.arny.allfy.utils.CallState
import com.arny.allfy.utils.CallStatus
import com.arny.allfy.utils.WebRTCClient
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.arny.allfy.R
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.UserViewModel

@Composable
fun CallScreen(
    conversationId: String,
    isCaller: Boolean,
    otherUserId: String,
    userViewModel: UserViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val eglBase = remember { EglBase.create() }
    var callState by remember { mutableStateOf(CallState(CallStatus.PENDING)) }
    var localVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var remoteVideoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    val userState by userViewModel.userState.collectAsState()

    LaunchedEffect(otherUserId) {
        userViewModel.getUserDetails(otherUserId)
    }

    val webRTCClient = remember(conversationId, isCaller) {
        WebRTCClient(
            context = context,
            eglBaseContext = eglBase.eglBaseContext,
            conversationId = conversationId,
            isCaller = isCaller,
            callerId = userState.currentUser.userId,
            onVideoTrackReceived = { track ->
                remoteVideoTrack = track
            },
            onStateChange = { state ->
                callState = state
                if (state.status == CallStatus.ENDED || state.status == CallStatus.ERROR) {
                    navController.popBackStack()
                }
            }
        )
    }

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true && permissions[Manifest.permission.RECORD_AUDIO] == true) {
            if (isCaller) {
                localVideoTrack = webRTCClient.getLocalVideoTrack()
                webRTCClient.startCall()
            }
        } else {
            Toast.makeText(
                context,
                "Camera and microphone permissions are required",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        } else if (isCaller) {
            localVideoTrack = webRTCClient.getLocalVideoTrack()
            webRTCClient.startCall()
        }
    }

    CallScreenContent(
        callState = callState,
        localVideoTrack = localVideoTrack,
        remoteVideoTrack = remoteVideoTrack,
        webRTCClient = webRTCClient,
        user = userState.otherUser,
        isCaller = isCaller,
        isLoading = userState.isLoadingOtherUser,
        requestPermissionsLauncher = requestPermissionsLauncher,
        context = context,
        eglBase = eglBase,
        navController = navController,
        onAcceptCall = {
            webRTCClient.acceptCall()
            localVideoTrack = webRTCClient.getLocalVideoTrack()
        }
    )

    DisposableEffect(callState.status) {
        onDispose {
            if (callState.status == CallStatus.ENDED || callState.status == CallStatus.ERROR) {
                webRTCClient.cleanup()
                eglBase.release()
            }
        }
    }
}
@Composable
fun CallScreenContent(
    callState: CallState,
    localVideoTrack: VideoTrack?,
    remoteVideoTrack: VideoTrack?,
    webRTCClient: WebRTCClient,
    user: User?,
    isCaller: Boolean,
    isLoading: Boolean,
    requestPermissionsLauncher: ActivityResultLauncher<Array<String>>,
    context: Context,
    eglBase: EglBase,
    navController: NavHostController,
    onAcceptCall: () -> Unit // Thêm callback cho Accept
) {
    Box(modifier = Modifier.fillMaxSize()) {
        remoteVideoTrack?.let { track ->
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        init(eglBase.eglBaseContext, null)
                        setEnableHardwareScaler(true)
                        setMirror(false)
                        setZOrderMediaOverlay(false)
                        track.addSink(this)
                    }
                },
                update = { renderer ->
                    renderer.release()
                    renderer.init(eglBase.eglBaseContext, null)
                    renderer.setEnableHardwareScaler(true)
                    renderer.setMirror(false)
                    renderer.setZOrderMediaOverlay(false)
                    track.addSink(renderer)
                }
            )
        }

        localVideoTrack?.let { track ->
            AndroidView(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black),
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        init(eglBase.eglBaseContext, null)
                        setEnableHardwareScaler(true)
                        setMirror(true)
                        setZOrderMediaOverlay(true)
                        track.addSink(this)
                    }
                },
                update = { renderer ->
                    renderer.release()
                    renderer.init(eglBase.eglBaseContext, null)
                    renderer.setEnableHardwareScaler(true)
                    renderer.setMirror(true)
                    renderer.setZOrderMediaOverlay(true)
                    track.addSink(renderer)
                }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                AsyncImage(
                    model = user?.imageUrl ?: "",
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    placeholder = painterResource(id = R.drawable.ic_user),
                    error = painterResource(id = R.drawable.ic_user)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = user?.username ?: "Unknown",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                text = when (callState.status) {
                    CallStatus.PENDING -> if (isCaller) "Calling..." else "Incoming Call"
                    CallStatus.CONNECTING -> "Connecting..."
                    CallStatus.CONNECTED -> "Connected"
                    CallStatus.ENDED -> "Call Ended"
                    CallStatus.ERROR -> "Error: ${callState.errorMessage ?: "Unknown"}"
                },
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        when (callState.status) {
            CallStatus.ERROR -> {
                Text(
                    text = "Error: ${callState.errorMessage ?: "Unknown error"}",
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
                Button(
                    onClick = { webRTCClient.endCall() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text("End Call")
                }
            }

            CallStatus.PENDING -> if (!isCaller) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Incoming Call from ${user?.username ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        Button(onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) != PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                requestPermissionsLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.CAMERA,
                                        Manifest.permission.RECORD_AUDIO
                                    )
                                )
                            } else {
                                onAcceptCall() // Sử dụng callback thay vì gọi trực tiếp
                            }
                        }) {
                            Text("Accept")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = { webRTCClient.rejectCall() }) {
                            Text("Reject")
                        }
                    }
                }
            }

            else -> {
                Button(
                    onClick = { webRTCClient.endCall() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text("End Call")
                }
            }
        }
    }
}
