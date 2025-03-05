package com.arny.allfy.presentation.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                        text = user.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
//                    if (user.isOnline) {
//                        Text(
//                            text = "Active now",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.primary
//                        )
//                    }
                    //Todo isOnline
                }
            }
        },
        actions = {
            IconButton(onClick = { /* Handle video call */ }) {
                Icon(Icons.Default.Call, "Video Call")
            }
            IconButton(onClick = { /* Handle more options */ }) {
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
                    color = if (isFromCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                        bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                    )
                )
                .padding(12.dp)
        ) {
            if (message.type == MessageType.TEXT) {
                Text(
                    text = message.content,
                    color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (message.type == MessageType.IMAGE) {
                Image(
                    painter = rememberAsyncImagePainter(message.content),
                    contentDescription = "Sent image",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            }
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

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navHostController: NavHostController,
    chatViewModel: ChatViewModel,
    userViewModel: UserViewModel,
    currentUserId: String,
    otherUserId: String
) {
    val messageInput by chatViewModel.messageInput.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val sendMessageState by chatViewModel.sendMessageState.collectAsState()
    val conversationState by chatViewModel.conversationState.collectAsState()
    val otherUserState by userViewModel.otherUser.collectAsState()

    LaunchedEffect(Unit) {
        chatViewModel.initializeChat(currentUserId, otherUserId)
        userViewModel.getUserById(otherUserId)
    }
    LaunchedEffect(Unit) {
        navHostController.currentBackStackEntry?.arguments?.let { args ->
            val conversationId = args.getString("conversationId")
            if (conversationId != null) {
                chatViewModel.initializeChat(currentUserId, otherUserId)
            }
        }
    }

    Scaffold(
        topBar = {
            if (otherUserState is Response.Success) {
                ChatTopBar(
                    user = (otherUserState as Response.Success<User>).data,
                    onBackClick = { navHostController.popBackStack() }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        ChatContent(
            paddingValues = paddingValues,
            conversationState = conversationState,
            otherUserState = otherUserState,
            messages = messages,
            messageInput = messageInput,
            currentUserId = currentUserId,
            sendMessageState = sendMessageState,
            onMessageRead = chatViewModel::markMessageAsRead,
            onMessageInputChanged = chatViewModel::onMessageInputChanged,
            onSendMessage = chatViewModel::sendMessage,
            onSendImages = chatViewModel::sendImages
        )
    }
}

@Composable
private fun ChatContent(
    paddingValues: PaddingValues,
    conversationState: Response<Conversation>,
    otherUserState: Response<User>,
    messages: List<Message>,
    messageInput: String,
    currentUserId: String,
    sendMessageState: Response<Boolean>,
    onMessageRead: (String, String) -> Unit,
    onMessageInputChanged: (String) -> Unit,
    onSendMessage: (String, Message) -> Unit,
    onSendImages: (String, List<Uri>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty()) {
                scope.launch {
                    if (conversationState is Response.Success) {
                        onSendImages(conversationState.data.id, uris)
                    }
                }
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
    ) {
        when {
            conversationState is Response.Loading || otherUserState is Response.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
            }

            conversationState is Response.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error loading chat",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = conversationState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            conversationState is Response.Success && otherUserState is Response.Success -> {
                val conversation = conversationState.data
                val otherUser = otherUserState.data

                ChatMessagesList(
                    messages = messages,
                    currentUserId = currentUserId,
                    conversationId = conversation.id,
                    onMessageRead = onMessageRead,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                ChatInput(
                    messageInput = messageInput,
                    sendMessageState = sendMessageState,
                    onMessageInputChanged = onMessageInputChanged,
                    onSendMessage = {
                        if (messageInput.isNotBlank()) {
                            val message = Message(
                                senderId = currentUserId, // Điền senderId ở đây
                                content = messageInput,
                                type = MessageType.TEXT
                            )
                            onSendMessage(conversation.id, message)
                        }
                    },
                    onImagePick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ChatMessagesList(
    messages: List<Message>,
    currentUserId: String,
    conversationId: String, // Thêm conversationId
    onMessageRead: (String, String) -> Unit, // Cập nhật tham số
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 16.dp),
        reverseLayout = true
    ) {
        items(
            items = messages.reversed(),
            key = { message -> message.id }
        ) { message ->
            LaunchedEffect(message.id) {
                if (message.senderId != currentUserId) {
                    onMessageRead(conversationId, message.id)
                }
            }
            ChatMessageItem(
                message = message,
                isFromCurrentUser = message.senderId == currentUserId
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
        }
    }
}


@Composable
fun ChatInput(
    messageInput: String,
    sendMessageState: Response<Boolean>,
    onMessageInputChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onImagePick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
    ) {
        if (sendMessageState is Response.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onImagePick,
                enabled = sendMessageState !is Response.Loading
            ) {
                Icon(Icons.Default.Add, "Attach Images")
            }
            TextField(
                value = messageInput,
                onValueChange = onMessageInputChanged,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                enabled = sendMessageState !is Response.Loading,
                placeholder = { Text("Message...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(
                onClick = onSendMessage,
                enabled = sendMessageState !is Response.Loading && messageInput.isNotBlank()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    "Send",
                    tint = if (messageInput.isNotBlank() && sendMessageState !is Response.Loading)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}