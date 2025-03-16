package com.arny.allfy.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.arny.allfy.R
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.utils.Response

@Composable
fun IncomingCallScreen(
    callerId: String,
    calleeId: String,
    userViewModel: UserViewModel,
    chatViewModel: ChatViewModel,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val callerState by userViewModel.otherUser.collectAsState()
    val conversationId = remember { listOf(callerId, calleeId).sorted().joinToString("_") }

    LaunchedEffect(Unit) {
        userViewModel.getUserById(callerId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (callerState) {
            is Response.Success -> {
                val caller = (callerState as Response.Success<User>).data
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(64.dp))
                        Image(
                            painter = rememberAsyncImagePainter(caller.imageUrl),
                            contentDescription = "Caller",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = caller.username,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Đang gọi...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Green
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = {
                                chatViewModel.rejectCall(
                                    conversationId,
                                    "callId_placeholder",
                                )
                                onReject()
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_call_end),
                                contentDescription = "Reject",
                                tint = MaterialTheme.colorScheme.onError
                            )
                        }
                        IconButton(
                            onClick = {
                                chatViewModel.acceptCall(
                                    conversationId,
                                    "callId_placeholder",
                                )
                                onAccept()
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.Green, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Accept",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            else -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}