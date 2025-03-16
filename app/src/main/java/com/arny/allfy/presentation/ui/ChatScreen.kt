package com.arny.allfy.presentation.ui

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
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
import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.Message
import com.arny.allfy.domain.model.MessageType
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.formatDuration
import com.arny.allfy.utils.formatTimestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.seconds

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

    val isLoading = conversationState is Response.Loading || otherUserState is Response.Loading

    Scaffold(
        topBar = {
            Column {
                ChatTopBar(
                    user = (otherUserState as? Response.Success)?.data ?: User(),
                    isLoading = isLoading,
                    onBackClick = { navHostController.popBackStack() },
                    onVoiceCallClick = {
                        chatViewModel.sendCallInvitation(currentUserId, otherUserId)
                        navHostController.navigate("call/$currentUserId/$otherUserId")
                    },
                    onVideoCallClick = { /* TODO: Handle video call */ }
                )
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
            onSendImages = chatViewModel::sendImages,
            onSendVoiceMessage = chatViewModel::sendVoiceMessage
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
                        text = "Online", // TODO: Thay bằng logic isOnline từ User
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
    onSendImages: (String, List<Uri>) -> Unit,
    onSendVoiceMessage: (String, Uri) -> Unit
) {
    val scope = rememberCoroutineScope()
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty() && conversationState is Response.Success) {
                scope.launch {
                    onSendImages(conversationState.data.id, uris)
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
            conversationState is Response.Error -> {
                ErrorState(conversationState.message)
            }

            conversationState is Response.Success && otherUserState is Response.Success -> {
                val conversation = conversationState.data
                ChatMessagesList(
                    messages = messages,
                    currentUserId = currentUserId,
                    conversationId = conversation.id,
                    onMessageRead = onMessageRead,
                    modifier = Modifier.weight(1f)
                )
                ChatInput(
                    messageInput = messageInput,
                    sendMessageState = sendMessageState,
                    onMessageInputChanged = onMessageInputChanged,
                    onSendMessage = {
                        if (messageInput.isNotBlank()) {
                            val message = Message(
                                senderId = currentUserId,
                                content = messageInput,
                                type = MessageType.TEXT
                            )
                            onSendMessage(conversation.id, message)
                        }
                    },
                    onImagePick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onSendVoiceMessage = { uri -> onSendVoiceMessage(conversation.id, uri) }
                )
            }
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
    conversationId: String,
    onMessageRead: (String, String) -> Unit,
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
            LaunchedEffect(message.id) {
                if (message.senderId != currentUserId) {
                    onMessageRead(conversationId, message.id)
                }
            }
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
    sendMessageState: Response<Boolean>,
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
        if (sendMessageState is Response.Loading) {
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
            IconButton(onClick = onImagePick, enabled = sendMessageState !is Response.Loading) {
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
                enabled = sendMessageState !is Response.Loading && !isRecording,
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
                    enabled = sendMessageState !is Response.Loading
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = "Record Voice",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onSendMessage,
                    enabled = sendMessageState !is Response.Loading && messageInput.isNotBlank()
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