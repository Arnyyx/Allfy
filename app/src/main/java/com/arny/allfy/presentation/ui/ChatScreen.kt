package com.arny.allfy.presentation.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.Toast
import com.arny.allfy.presentation.state.ChatUiState
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.utils.formatTimestamp
import okhttp3.internal.wait

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    otherUser: User,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val messageInput by viewModel.messageInput.collectAsState()
    val messages by viewModel.messages.collectAsState()

    LaunchedEffect(otherUser.userID) {
        viewModel.observeMessages(otherUser.userID)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ChatTopBar(user = otherUser, onBackClick = onBackClick)

        Box(modifier = Modifier.weight(1f)) {
            ChatMessagesList(
                messages = messages,
                currentUserId = viewModel.currentUserId,
                onMessageRead = { messageId ->
                    viewModel.markMessageAsRead(messageId)
                }
            )
            when (uiState) {
                is ChatUiState.Initial -> {
//                    LinearProgressIndicator(
//                        modifier = Modifier.fillMaxWidth()
//                    )
                }

                is ChatUiState.Success -> {
                    viewModel.observeMessages(otherUser.userID)
                }

                is ChatUiState.Error -> {
                    Text(
                        text = (uiState as ChatUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                else -> {}
            }
        }

        ChatInput(
            value = messageInput,
            onValueChange = viewModel::onMessageInputChanged,
            onSendClick = {
                if (messageInput.isNotBlank()) {
                    viewModel.sendMessage(otherUser.userID, messageInput)
                }
            },
            isEnabled = uiState !is ChatUiState.SendingMessage
        )

        if (uiState is ChatUiState.SendingMessage) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    user: User,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(user.imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = user.userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (user.isOnline) {
                        Text(
                            text = "Active now",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = { /* Handle video call */ }) {
                Icon(Icons.Default.Call, "Video Call")
            }
            IconButton(onClick = { /* Handle voice call */ }) {
                Icon(Icons.Default.Call, "Voice Call")
            }
        }
    )
}

@Composable
fun ChatMessagesList(
    messages: List<Message>,
    currentUserId: String,
    onMessageRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        reverseLayout = true
    ) {
        items(messages.reversed()) { message ->
            // Mark message as read if it's not from current user
            LaunchedEffect(message.id) {
                if (!message.isRead && message.senderId != currentUserId) {
                    onMessageRead(message.id)
                }
            }

            ChatMessageItem(
                message = message,
                isFromCurrentUser = message.senderId == currentUserId
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ChatMessageItem(
    message: Message,
    isFromCurrentUser: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isFromCurrentUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                        bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = if (isFromCurrentUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isFromCurrentUser && message.isRead) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Read",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .imePadding()
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { /* Handle attachment */ },
            enabled = isEnabled
        ) {
            Icon(Icons.Default.Add, "Attach")
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            enabled = isEnabled,
            placeholder = { Text("Message...") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(24.dp)
        )
        IconButton(
            onClick = onSendClick,
            enabled = isEnabled && value.isNotBlank()
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                "Send",
                tint = if (value.isNotBlank() && isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}