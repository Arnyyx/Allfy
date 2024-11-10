package com.arny.allfy.presentation.ui

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.pullToRefreshIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.model.MessageType
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.Toast
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.formatTimestamp
import kotlinx.coroutines.launch

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
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
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
                Icon(Icons.Default.MoreVert, "More")
            }
        }
    )
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
                disabledContainerColor = Color.Transparent
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

//New
@Composable
fun ChatScreen(
    navHostController: NavHostController,
    chatViewModel: ChatViewModel,
    userViewModel: UserViewModel,
    currentUserId: String,
    otherUserId: String
) {
    // State management
    val messageInput by chatViewModel.messageInput.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val sendMessageState by chatViewModel.sendMessageState.collectAsState()
    val currentUserState by userViewModel.currentUser.collectAsState()
    val conversationState by chatViewModel.conversationState.collectAsState()
    val otherUserState by userViewModel.otherUser.collectAsState()

    val currentUser = remember(currentUserState) {
        (currentUserState as? Response.Success<User>)?.data
    }

    LaunchedEffect(currentUserId, otherUserId) {
        // Load data in parallel
        launch { chatViewModel.initializeChat(currentUserId, otherUserId) }
        launch { userViewModel.getUserById(otherUserId) }
    }

    // Main content
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // Show loading state
            conversationState is Response.Loading || currentUserState is Response.Loading -> {
                LoadingScreen()
            }

            // Show error state
            conversationState is Response.Error -> {
                ErrorScreen(
                    message = (conversationState as Response.Error).message,
                    onRetry = {
                        chatViewModel.initializeChat(currentUserId, otherUserId)
                    }
                )
            }

            // Show main chat content
            conversationState is Response.Success && currentUser != null -> {
                val conversation = (conversationState as Response.Success<Conversation>).data

                // Load messages when conversation is ready
                LaunchedEffect(conversation.id) {
                    chatViewModel.loadMessages(conversation.id)
                }

                ChatContent(
                    otherUserState = otherUserState,
                    messages = messages,
                    messageInput = messageInput,
                    currentUser = currentUser,
                    sendMessageState = sendMessageState,
                    onBackClick = { navHostController.popBackStack() },
                    onMessageRead = { messageId -> chatViewModel.markMessageAsRead(messageId) },
                    onMessageInputChanged = chatViewModel::onMessageInputChanged,
                    onSendMessage = { message ->
                        chatViewModel.sendMessage(conversation.id, message)
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading chat...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Error loading chat",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatContent(
    otherUserState: Response<User>,
    messages: List<Message>,
    messageInput: String,
    currentUser: User,
    sendMessageState: Response<Boolean>,
    onBackClick: () -> Unit,
    onMessageRead: (String) -> Unit,
    onMessageInputChanged: (String) -> Unit,
    onSendMessage: (Message) -> Unit
) {
    when (otherUserState) {
        is Response.Success -> {
            val otherUser = otherUserState.data
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Top bar with user info
                ChatTopBar(
                    user = otherUser,
                    onBackClick = onBackClick
                )

                Box(modifier = Modifier.weight(1f)) {

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ChatMessagesList(
                            messages = messages,
                            currentUserId = currentUser.userID,
                            onMessageRead = onMessageRead
                        )
                    }
                }

                // Input section with loading indicator
                Column {
                    if (sendMessageState is Response.Loading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ChatInput(
                        value = messageInput,
                        onValueChange = onMessageInputChanged,
                        onSendClick = {
                            if (messageInput.isNotBlank()) {
                                val message = Message(
                                    senderId = currentUser.userID,
                                    receiverId = otherUser.userID,
                                    content = messageInput,
                                    isRead = false,
                                    type = MessageType.TEXT
                                )
                                onSendMessage(message)
                            }
                        },
                        isEnabled = sendMessageState !is Response.Loading
                    )
                }
            }
        }

        is Response.Error -> {
            Toast(otherUserState.message)
        }

        is Response.Loading -> {
            LoadingScreen()
        }
    }
}

// Optimized ChatMessagesList
@Composable
fun ChatMessagesList(
    messages: List<Message>,
    currentUserId: String,
    onMessageRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        reverseLayout = true
    ) {
        items(
            items = messages.reversed(),
            key = { message -> message.id }  // Add key for better performance
        ) { message ->
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

    // Auto scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
        }
    }
}