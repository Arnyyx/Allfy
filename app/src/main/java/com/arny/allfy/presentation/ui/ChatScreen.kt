package com.arny.allfy.presentation.ui

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.SubcomposeAsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.model.MessageType
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.state.ChatState
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Screen
import com.arny.allfy.utils.formatDuration
import com.arny.allfy.utils.formatTimestamp
import com.arny.allfy.utils.getDataOrNull
import com.arny.allfy.utils.getErrorMessageOrNull
import com.arny.allfy.utils.isError
import com.arny.allfy.utils.isLoading
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

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

    var isConversationInitialized by remember { mutableStateOf(conversationId != null) }

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
            chatState.sendVoiceMessageState.isLoading

    val error = chatState.loadConversationsState.getErrorMessageOrNull()
        ?: chatState.sendMessageState.getErrorMessageOrNull()
        ?: chatState.sendImagesState.getErrorMessageOrNull()
        ?: chatState.sendVoiceMessageState.getErrorMessageOrNull()
        ?: chatState.initializeConversationState.getErrorMessageOrNull()

    Scaffold(
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
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        ChatContent(
            chatState = chatState,
            paddingValues = paddingValues,
            messages = chatState.messages,
            messageInput = chatState.messageInput,
            currentUserId = currentUserId,
            conversationId = conversationId,
            sendMessageState = isLoading,
            onMessageInputChanged = chatViewModel::onMessageInputChanged,
            onSendMessage = { message ->
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
            }
        )
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
            .error(R.drawable.ic_user) // Hiển thị khi lỗi
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
    onMessageInputChanged: (String) -> Unit,
    onSendMessage: (Message) -> Unit,
    onSendImages: (List<Uri>) -> Unit,
    onSendVoiceMessage: (Uri) -> Unit
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
                modifier = Modifier.weight(1f)
            )
            ChatInput(
                messageInput = messageInput,
                isSendingMessage = sendMessageState,
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
                onSendVoiceMessage = onSendVoiceMessage
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
    modifier: Modifier = Modifier
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
                isFromCurrentUser = message.senderId == currentUserId
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
    isFromCurrentUser: Boolean
) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    var isPlaying by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { it / 2 },
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            text = message.content,
                            color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
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
                                            Uri.parse(message.content)
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
    onMessageInputChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onImagePick: () -> Unit,
    onSendVoiceMessage: (Uri) -> Unit
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
        if (isSendingMessage) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onImagePick, enabled = !isSendingMessage) {
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
                placeholder = { Text("Type a message...") },
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
                    enabled = !isSendingMessage
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
