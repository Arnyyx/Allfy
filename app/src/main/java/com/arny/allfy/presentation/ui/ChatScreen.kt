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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    user: User,
    onBackClick: () -> Unit,
    onVoiceCallClick: () -> Unit,
    onVideoCallClick: () -> Unit
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
                    // Todo: isOnline (giữ nguyên)
                }
            }
        },
        actions = {
            IconButton(onClick = onVoiceCallClick) {
                Icon(Icons.Default.Call, "Voice Call")
            }
            IconButton(onClick = onVideoCallClick) {
                Icon(painterResource(R.drawable.ic_videocall), "Video Call")
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
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    var isPlaying by remember { mutableStateOf(false) }

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
                .padding(12.dp)
        ) {
            when (message.type) {
                MessageType.TEXT -> {
                    Text(
                        text = message.content,
                        color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                MessageType.IMAGE -> {
                    Image(
                        painter = rememberAsyncImagePainter(message.content),
                        contentDescription = "Sent image",
                        modifier = Modifier.size(200.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                MessageType.VOICE -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    mediaPlayer.stop()
                                    mediaPlayer.reset()
                                    isPlaying = false
                                } else {
                                    mediaPlayer.setDataSource(context, Uri.parse(message.content))
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
                }

                MessageType.VIDEO -> {
                    // TODO: Xử lý video (nếu cần)
                    Text(text = "Video message (TODO)")
                }

                MessageType.FILE -> {
                    // TODO: Xử lý file (nếu cần)
                    Text(text = "File message (TODO)")
                }
            }
        }
        Text(
            text = formatTimestamp(message.timestamp),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
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
fun Painter.toImageVector(): ImageVector {
    return remember(this) {
        ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
            .apply {
            }
            .build()
    }
}

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


    Scaffold(
        topBar = {
            if (otherUserState is Response.Success && conversationState is Response.Success) {
                ChatTopBar(
                    user = (otherUserState as Response.Success<User>).data,
                    onBackClick = { navHostController.popBackStack() },
                    onVoiceCallClick = {

                    },
                    onVideoCallClick = {

                    }
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
            onSendImages = chatViewModel::sendImages,
            onSendVoiceMessage = chatViewModel::sendVoiceMessage
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
    onSendImages: (String, List<Uri>) -> Unit,
    onSendVoiceMessage: (String, Uri) -> Unit
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
                    onSendVoiceMessage = { uri ->
                        onSendVoiceMessage(conversation.id, uri)
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
    conversationId: String,
    onMessageRead: (String, String) -> Unit,
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
    onImagePick: () -> Unit,
    onSendVoiceMessage: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableLongStateOf(0L) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFileUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher để xin quyền RECORD_AUDIO
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

    // Giải phóng MediaRecorder khi Composable bị hủy
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
                enabled = sendMessageState !is Response.Loading && !isRecording,
                placeholder = { Text("Message...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )

            if (!isRecording) {
                IconButton(
                    onClick = {
                        // Xin quyền trước khi ghi âm
                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    enabled = sendMessageState !is Response.Loading
                ) {
                    Icon(painterResource(R.drawable.ic_mic), "Record Voice")
                }
                IconButton(
                    onClick = onSendMessage,
                    enabled = sendMessageState !is Response.Loading && messageInput.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send")
                }
            } else {
                Text(
                    text = formatDuration(recordingDuration),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 8.dp)
                )
                IconButton(
                    onClick = {
                        isRecording = false
                        mediaRecorder?.stop()
                        mediaRecorder?.release()
                        mediaRecorder = null
                        recordingDuration = 0L
                        audioFileUri?.let { uri ->
                            onSendVoiceMessage(uri)
                        }
                    }
                ) {
                    Icon(painterResource(R.drawable.ic_send), "Stop Recording")
                }
                IconButton(
                    onClick = {
                        isRecording = false
                        mediaRecorder?.stop()
                        mediaRecorder?.release()
                        mediaRecorder = null
                        recordingDuration = 0L
                        audioFileUri?.let { uri ->
                            File(uri.path!!).delete()
                        }
                        audioFileUri = null
                    }
                ) {
                    Icon(painterResource(R.drawable.ic_cancel), "Cancel Recording")
                }
            }
        }
    }
}


