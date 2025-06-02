package com.arny.allfy.presentation.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.arny.allfy.R
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.CallViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.getDataOrNull
import com.arny.allfy.utils.isLoading
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun CallScreen(
    conversationId: String,
    isCaller: Boolean,
    otherUserId: String,
    userViewModel: UserViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val callViewModel: CallViewModel = hiltViewModel()
    val callState by callViewModel.callState.collectAsState()
    val localVideoTrack by callViewModel.localVideoTrack.collectAsState()
    val remoteVideoTrack by callViewModel.remoteVideoTrack.collectAsState()
    val userState by userViewModel.userState.collectAsState()

    // Extract current user from new state structure
    val currentUser = userState.currentUserState.getDataOrNull()

    LaunchedEffect(Unit) {
        callViewModel.initialize(
            conversationId = conversationId,
            isCaller = isCaller,
            callerId = currentUser?.userId ?: ""
        )
    }

    LaunchedEffect(otherUserId) {
        userViewModel.getUserDetails(otherUserId)
    }

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.CAMERA] == true && permissions[android.Manifest.permission.RECORD_AUDIO] == true) {
            if (isCaller) {
                callViewModel.startCall()
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
        if (!callViewModel.hasPermissions()) {
            requestPermissionsLauncher.launch(
                arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO
                )
            )
        } else if (isCaller) {
            callViewModel.startCall()
        }
    }

    CallScreenContent(
        callState = callState,
        localVideoTrack = localVideoTrack,
        remoteVideoTrack = remoteVideoTrack,
        user = userState.otherUserState.getDataOrNull(),
        isCaller = isCaller,
        isLoading = userState.otherUserState.isLoading,
        requestPermissionsLauncher = requestPermissionsLauncher,
        context = context,
        callViewModel = callViewModel,
        navController = navController,
        onAcceptCall = { callViewModel.acceptCall() },
        onRejectCall = { callViewModel.rejectCall() },
        onEndCall = { callViewModel.endCall() }
    )
}

@Composable
fun CallScreenContent(
    callState: com.arny.allfy.utils.CallState,
    localVideoTrack: VideoTrack?,
    remoteVideoTrack: VideoTrack?,
    user: User?,
    isCaller: Boolean,
    isLoading: Boolean,
    requestPermissionsLauncher: ActivityResultLauncher<Array<String>>,
    context: Context,
    callViewModel: CallViewModel,
    navController: NavHostController,
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onEndCall: () -> Unit
) {
    val eglBase = remember { EglBase.create() }

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
                    com.arny.allfy.utils.CallStatus.PENDING -> if (isCaller) "Calling..." else "Incoming Call"
                    com.arny.allfy.utils.CallStatus.CONNECTING -> "Connecting..."
                    com.arny.allfy.utils.CallStatus.CONNECTED -> "Connected"
                    com.arny.allfy.utils.CallStatus.ENDED -> "Call Ended"
                    com.arny.allfy.utils.CallStatus.ERROR -> "Error: ${callState.errorMessage ?: "Unknown"}"
                },
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        when (callState.status) {
            com.arny.allfy.utils.CallStatus.ERROR -> {
                Text(
                    text = "Error: ${callState.errorMessage ?: "Unknown error"}",
                    color = Color.Red,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
                Button(
                    onClick = onEndCall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text("End Call")
                }
            }

            com.arny.allfy.utils.CallStatus.PENDING -> if (!isCaller) {
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
                            if (!callViewModel.hasPermissions()) {
                                requestPermissionsLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.CAMERA,
                                        android.Manifest.permission.RECORD_AUDIO
                                    )
                                )
                            } else {
                                onAcceptCall()
                            }
                        }) {
                            Text("Accept")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = onRejectCall) {
                            Text("Reject")
                        }
                    }
                }
            }

            else -> {
                Button(
                    onClick = onEndCall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text("End Call")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            eglBase.release()
        }
    }
}