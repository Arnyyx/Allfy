package com.arny.allfy.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.arny.allfy.R
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.formatDuration
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    navHostController: NavHostController,
    userViewModel: UserViewModel,
    chatViewModel: ChatViewModel,
    currentUserId: String,
    otherUserId: String
) {
    val otherUserState by userViewModel.otherUser.collectAsState()
    val callState by chatViewModel.callState.collectAsState()
    var callDuration by remember { mutableLongStateOf(0L) }
    var isMicMuted by remember { mutableStateOf(false) }
    val conversationId = remember { listOf(currentUserId, otherUserId).sorted().joinToString("_") }

    LaunchedEffect(callState) {
        if (callState == "accepted") {
            while (callState == "accepted") {
                delay(1000)
                callDuration += 1000
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        when (otherUserState) {
            is Response.Success -> {
                val user = (otherUserState as Response.Success<User>).data
                CallContent(
                    user = user,
                    isCallConnected = callState == "accepted",
                    callDuration = callDuration,
                    isMicMuted = isMicMuted,
                    callState = callState,
                    onEndCall = {
                        if (callState == "pending") {
                            chatViewModel.cancelCall(conversationId)
                        }
                        chatViewModel.endCall(conversationId, callDuration)
                        navHostController.popBackStack()
                    },
                    onToggleMic = { isMicMuted = !isMicMuted }
                )
            }
            else -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun CallContent(
    user: User,
    isCallConnected: Boolean,
    callDuration: Long,
    isMicMuted: Boolean,
    callState: String,
    onEndCall: () -> Unit,
    onToggleMic: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = rememberAsyncImagePainter(user.imageUrl),
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = user.username,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (callState) {
                    "idle" -> "Cuộc gọi đã kết thúc"
                    "pending" -> "Đang gọi..."
                    "accepted" -> formatDuration(callDuration)
                    "rejected" -> "Cuộc gọi bị từ chối"
                    else -> "Unknown state"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCallConnected) Color.Green else MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = onToggleMic,
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = if (isMicMuted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    painter = if (isMicMuted) painterResource(id = R.drawable.ic_mic_off)
                    else painterResource(id = R.drawable.ic_mic),
                    contentDescription = if (isMicMuted) "Unmute" else "Mute",
                    tint = if (isMicMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                )
            }
            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_call_end),
                    contentDescription = "End Call",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}