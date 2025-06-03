package com.arny.allfy.presentation.ui

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.model.MessageType
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.Dialog
import com.arny.allfy.presentation.state.ChatState
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.net.toUri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset

@Composable
fun ChatScreen(
    navHostController: NavHostController,
    chatViewModel: ChatViewModel,
    userViewModel: UserViewModel,
    conversationId: String?,
    otherUserId: String,
) {
    val chatState by chatViewModel.chatState.collectAsState()
    val userState by userViewModel.userState.collectAsState()
    val currentUserId = userState.currentUserState.getDataOrNull()?.userId ?: ""
    val context = LocalContext.current

    var isConversationInitialized by remember { mutableStateOf(conversationId != null) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var showOriginalContentDialog by remember { mutableStateOf<Message?>(null) }

    LaunchedEffect(conversationId, currentUserId, otherUserId) {
        if (conversationId != null) {
            chatViewModel.loadMessages(conversationId)
        } else if (currentUserId.isNotEmpty()) {
            chatViewModel.loadConversations(currentUserId)
        }
        userViewModel.getUserDetails(otherUserId)
    }

    val isLoading = chatState.loadConversationsState.isLoading ||
            userState.otherUserState.isLoading ||
            chatState.sendMessageState.isLoading ||
            chatState.sendImagesState.isLoading ||
            chatState.sendVoiceMessageState.isLoading ||
            chatState.deleteMessageState.isLoading ||
            chatState.editMessageState.isLoading

    val error = chatState.loadConversationsState.getErrorMessageOrNull()
        ?: chatState.sendMessageState.getErrorMessageOrNull()
        ?: chatState.sendImagesState.getErrorMessageOrNull()
        ?: chatState.sendVoiceMessageState.getErrorMessageOrNull()
        ?: chatState.initializeConversationState.getErrorMessageOrNull()
        ?: chatState.deleteMessageState.getErrorMessageOrNull()
        ?: chatState.editMessageState.getErrorMessageOrNull()

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            Column {
                ChatTopBar(
                    user = userState.otherUserState.getDataOrNull() ?: User(),
                    isLoading = isLoading,
                    onBackClick = { navHostController.popBackStack() },
                    onVoiceCallClick = {
                        if (conversationId != null) {
                            navHostController.navigate(
                                Screen.CallScreen(
                                    conversationId = conversationId,
                                    otherUserId = otherUserId,
                                    isCaller = true
                                )
                            )
                        }
                    },
                    onVideoCallClick = {
                        if (conversationId != null) {
                            // TODO: Implement video call navigation
                        }
                    }
                )
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        ChatContent(
            chatState = chatState,
            paddingValues = paddingValues,
            messages = chatState.messages,
            messageInput = chatState.messageInput,
            currentUserId = currentUserId,
            conversationId = conversationId,
            sendMessageState = isLoading,
            editingMessage = editingMessage,
            onMessageInputChanged = chatViewModel::onMessageInputChanged,
            onSendMessage = { message ->
                if (editingMessage != null) {
                    val conversations =
                        chatState.loadConversationsState.getDataOrNull() ?: emptyList()
                    val converId = conversationId ?: conversations.firstOrNull {
                        it.participants.containsAll(listOf(currentUserId, otherUserId))
                    }?.id ?: return@ChatContent
                    chatViewModel.editMessage(converId, editingMessage!!.id, message.content)
                    editingMessage = null
                } else {
                    if (!isConversationInitialized) {
                        chatViewModel.initializeConversation(listOf(currentUserId, otherUserId))
                        isConversationInitialized = true
                        if (!chatState.initializeConversationState.isLoading) {
                            val conversations =
                                chatState.loadConversationsState.getDataOrNull() ?: emptyList()
                            val converId = conversations.firstOrNull {
                                it.participants.containsAll(listOf(currentUserId, otherUserId))
                            }?.id ?: return@ChatContent
                            chatViewModel.sendMessage(converId, message)
                        }
                    } else {
                        val conversations =
                            chatState.loadConversationsState.getDataOrNull() ?: emptyList()
                        val converId = conversationId ?: conversations.firstOrNull {
                            it.participants.containsAll(listOf(currentUserId, otherUserId))
                        }?.id ?: return@ChatContent
                        chatViewModel.sendMessage(converId, message)
                    }
                }
            },
            onSendImages = { uris ->
                val conversations = chatState.loadConversationsState.getDataOrNull() ?: emptyList()
                val converId = conversationId ?: conversations.firstOrNull {
                    it.participants.containsAll(listOf(currentUserId, otherUserId))
                }?.id
                if (converId != null) {
                    chatViewModel.sendImages(converId, uris)
                }
            },
            onSendVoiceMessage = { uri ->
                val conversations = chatState.loadConversationsState.getDataOrNull() ?: emptyList()
                val converId = conversationId ?: conversations.firstOrNull {
                    it.participants.containsAll(listOf(currentUserId, otherUserId))
                }?.id
                if (converId != null) {
                    chatViewModel.sendVoiceMessage(converId, uri)
                }
            },
            onDeleteMessage = { messageId ->
                showDeleteDialog = messageId
            },
            onEditMessage = { message ->
                if (message.senderId == currentUserId && message.type == MessageType.TEXT) {
                    editingMessage = message
                    chatViewModel.onMessageInputChanged(message.content)
                }
            },
            onCancelEdit = { editingMessage = null },
            onShowOriginalContent = { message ->
                if (message.editedTimestamp != null && message.originalContent != null) {
                    showOriginalContentDialog = message
                }
            }
        )
        if (showDeleteDialog != null) {
            Dialog(
                title = "Delete Message",
                message = "Are you sure you want to delete this message?",
                confirmText = "Delete",
                dismissText = "Cancel",
                onConfirm = {
                    showDeleteDialog?.let { messageId ->
                        val conversations =
                            chatState.loadConversationsState.getDataOrNull() ?: emptyList()
                        val converId = conversationId ?: conversations.firstOrNull {
                            it.participants.containsAll(listOf(currentUserId, otherUserId))
                        }?.id
                        if (converId != null) {
                            chatViewModel.deleteMessage(converId, messageId)
                        }
                        showDeleteDialog = null
                    }
                },
                onDismiss = { showDeleteDialog = null }
            )
        }
        if (showOriginalContentDialog != null) {
            Dialog(
                title = "Original Message",
                message = showOriginalContentDialog?.originalContent ?: "",
                confirmText = "OK",
                onConfirm = { showOriginalContentDialog = null },
                onDismiss = { showOriginalContentDialog = null }
            )
        }
    }

    LaunchedEffect(chatState.deleteMessageState) {
        if (chatState.deleteMessageState is Response.Success) {
            chatViewModel.resetDeleteMessageState()
            Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(chatState.editMessageState) {
        if (chatState.editMessageState is Response.Success) {
            chatViewModel.resetEditMessageState()
            Toast.makeText(context, "Message edited", Toast.LENGTH_SHORT).show()
        } else if (chatState.editMessageState.isError) {
            Toast.makeText(
                context,
                chatState.editMessageState.getErrorMessageOrNull() ?: "Failed to edit message",
                Toast.LENGTH_SHORT
            ).show()
            chatViewModel.resetEditMessageState()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    user: User,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImageWithPlaceholder(
                    imageUrl = user.imageUrl,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = user.username.ifBlank { "Loading..." },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Online",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            IconButton(onClick = onVoiceCallClick, enabled = !isLoading) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Voice Call",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onVideoCallClick, enabled = !isLoading) {
                Icon(
                    painter = painterResource(R.drawable.ic_videocall),
                    contentDescription = "Video Call",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = { /* TODO: More options */ }, enabled = !isLoading) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun AsyncImageWithPlaceholder(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .placeholder(R.drawable.ic_user)
            .error(R.drawable.ic_user)
            .build()
    )
    Image(
        painter = painter,
        contentDescription = "User avatar",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun ChatContent(
    chatState: ChatState,
    paddingValues: PaddingValues,
    messages: List<Message>,
    messageInput: String,
    currentUserId: String,
    conversationId: String?,
    sendMessageState: Boolean,
    editingMessage: Message?,
    onMessageInputChanged: (String) -> Unit,
    onSendMessage: (Message) -> Unit,
    onSendImages: (List<Uri>) -> Unit,
    onSendVoiceMessage: (Uri) -> Unit,
    onDeleteMessage: (String) -> Unit,
    onEditMessage: (Message) -> Unit,
    onShowOriginalContent: (Message) -> Unit,
    onCancelEdit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty()) {
                scope.launch {
                    onSendImages(uris)
                }
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(paddingValues)
    ) {
        val conversationError = chatState.loadConversationsState.getErrorMessageOrNull()
        if (conversationError != null) {
            ErrorState(conversationError)
        } else {
            ChatMessagesList(
                messages = messages,
                currentUserId = currentUserId,
                modifier = Modifier.weight(1f),
                onDeleteMessage = onDeleteMessage,
                onEditMessage = onEditMessage,
                onShowOriginalContent = onShowOriginalContent
            )
            ChatInput(
                messageInput = messageInput,
                isSendingMessage = sendMessageState,
                isEditing = editingMessage != null,
                onMessageInputChanged = onMessageInputChanged,
                onSendMessage = {
                    if (messageInput.isNotBlank()) {
                        val message = Message(
                            senderId = currentUserId,
                            content = messageInput,
                            type = MessageType.TEXT
                        )
                        onSendMessage(message)
                    }
                },
                onImagePick = {
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onSendVoiceMessage = onSendVoiceMessage,
                onCancelEdit = onCancelEdit
            )
        }
    }
}

@Composable
private fun ErrorState(errorMessage: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error loading chat",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ChatMessagesList(
    messages: List<Message>,
    currentUserId: String,
    modifier: Modifier = Modifier,
    onDeleteMessage: (String) -> Unit,
    onEditMessage: (Message) -> Unit,
    onShowOriginalContent: (Message) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 16.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = messages.reversed(),
            key = { it.id }
        ) { message ->
            AnimatedMessageItem(
                message = message,
                isFromCurrentUser = message.senderId == currentUserId,
                currentUserId = currentUserId,
                onDeleteMessage = onDeleteMessage,
                onEditMessage = onEditMessage,
                onShowOriginalContent = onShowOriginalContent
            )
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && listState.firstVisibleItemIndex <= 1) {
            scope.launch {
                listState.animateScrollToItem(0)
            }
        }
    }
}

@Composable
private fun AnimatedMessageItem(
    message: Message,
    isFromCurrentUser: Boolean,
    currentUserId: String,
    onDeleteMessage: (String) -> Unit,
    onEditMessage: (Message) -> Unit,
    onShowOriginalContent: (Message) -> Unit
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    val density = LocalDensity.current

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { it / 2 },
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            if (isFromCurrentUser) {
                                showMenu = true
                                menuOffset = offset
                            }
                        }
                    )
                },
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = if (isFromCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                when (message.type) {
                    MessageType.TEXT -> {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = message.content,
                                color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (message.editedTimestamp != null) {
                                TextButton(
                                    onClick = { onShowOriginalContent(message) },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        text = "Edited",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary.copy(
                                            alpha = 0.7f
                                        ) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    MessageType.IMAGE -> {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(message.content)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Sent image",
                            modifier = Modifier
                                .wrapContentSize()
                                .sizeIn(maxWidth = 200.dp, maxHeight = 200.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    MessageType.VOICE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        mediaPlayer.stop()
                                        mediaPlayer.reset()
                                        isPlaying = false
                                    } else {
                                        mediaPlayer.setDataSource(
                                            context,
                                            message.content.toUri()
                                        )
                                        mediaPlayer.prepare()
                                        mediaPlayer.start()
                                        isPlaying = true
                                        mediaPlayer.setOnCompletionListener {
                                            isPlaying = false
                                            mediaPlayer.reset()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                                    contentDescription = if (isPlaying) "Pause" else "Play"
                                )
                            }
                            Text(
                                text = "Voice message",
                                color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    MessageType.VOICE_CALL, MessageType.VIDEO_CALL -> {
                        val icon = if (message.type == MessageType.VOICE_CALL)
                            painterResource(id = R.drawable.ic_call)
                        else
                            painterResource(id = R.drawable.ic_videocall)
                        val callTypeText =
                            if (message.type == MessageType.VOICE_CALL) "Voice call" else "Video call"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                painter = icon,
                                contentDescription = callTypeText,
                                tint = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = callTypeText,
                                    color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = message.content,
                                    color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary.copy(
                                        alpha = 0.7f
                                    ) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    MessageType.VIDEO, MessageType.FILE -> {
                        Text(text = "${message.type.name} message (TODO)")
                    }
                }
            }
            if (isFromCurrentUser) {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    offset = with(density) {
                        DpOffset(
                            x = menuOffset.x.toDp(),
                            y = menuOffset.y.toDp() - 50.dp
                        )
                    }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            if (message.type == MessageType.TEXT) {
                                onEditMessage(message)
                            }
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        enabled = message.type == MessageType.TEXT
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDeleteMessage(message.id)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
    }
}

@Composable
private fun ChatInput(
    messageInput: String,
    isSendingMessage: Boolean,
    isEditing: Boolean,
    onMessageInputChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onImagePick: () -> Unit,
    onSendVoiceMessage: (Uri) -> Unit,
    onCancelEdit: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableLongStateOf(0L) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFileUri by remember { mutableStateOf<Uri?>(null) }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isRecording = true
            val audioFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.mp3")
            audioFileUri = Uri.fromFile(audioFile)
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            scope.launch {
                while (isRecording) {
                    delay(1000L)
                    recordingDuration += 1000L
                }
            }
        } else {
            Toast.makeText(context, "Record permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Editing message",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onCancelEdit) {
                    Text("Cancel")
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onImagePick,
                enabled = !isSendingMessage && !isRecording && !isEditing
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach Images",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            TextField(
                value = messageInput,
                onValueChange = onMessageInputChanged,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                enabled = !isSendingMessage && !isRecording,
                placeholder = { Text(if (isEditing) "Edit message..." else "Type a message...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )
            if (!isRecording) {
                IconButton(
                    onClick = { recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    enabled = !isSendingMessage && !isEditing
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = "Record Voice",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onSendMessage,
                    enabled = !isSendingMessage && messageInput.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = formatDuration(recordingDuration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(onClick = {
                    isRecording = false
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                    mediaRecorder = null
                    recordingDuration = 0L
                    audioFileUri?.let { onSendVoiceMessage(it) }
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = "Stop Recording",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = {
                    isRecording = false
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                    mediaRecorder = null
                    recordingDuration = 0L
                    audioFileUri?.let { File(it.path!!).delete() }
                    audioFileUri = null
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cancel),
                        contentDescription = "Cancel Recording",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}