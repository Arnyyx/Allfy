package com.arny.allfy.presentation.ui

import android.util.Log
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.Story
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.Dialog
import com.arny.allfy.presentation.viewmodel.StoryViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.getDataOrNull
import com.arny.allfy.utils.toTimeAgo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoryViewerScreen(
    navController: NavController,
    storyViewModel: StoryViewModel,
    userViewModel: UserViewModel,
    userId: String,
    isCurrentUser: Boolean,
) {
    val storyState by storyViewModel.storyState.collectAsState()
    val userState by userViewModel.userState.collectAsState()
    var currentStoryIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        storyViewModel.getUserStories(userId)
        if (isCurrentUser) {
            userViewModel.getCurrentUser(userId)
        } else {
            userViewModel.getUserDetails(userId)
        }
    }

    LaunchedEffect(currentStoryIndex, isPaused) {
        val stories = (storyState.userStoriesState as? Response.Success)?.data ?: emptyList()
        if (stories.isNotEmpty() && currentStoryIndex < stories.size && !isPaused) {
            val currentStory = stories[currentStoryIndex]
            val currentUserId =
                userViewModel.userState.value.currentUserState.getDataOrNull()?.userId ?: ""

            storyViewModel.logStoryView(currentUserId, currentStory.storyID)

            val duration = when (currentStory.mediaType) {
                "video" -> currentStory.maxVideoDuration ?: 15000L
                else -> currentStory.imageDuration ?: 5000L
            }

            val stepTime = 50L
            val totalSteps = duration / stepTime

            for (step in 0..totalSteps) {
                if (!isPaused) {
                    progress = step.toFloat() / totalSteps
                    delay(stepTime)
                }
            }

            if (currentStoryIndex < stories.size - 1) {
                currentStoryIndex++
                progress = 0f
            } else {
                navController.popBackStack()
            }
        }
    }

    LaunchedEffect(storyState.deleteStoryState) {
        when (storyState.deleteStoryState) {
            is Response.Success -> {
                storyViewModel.resetDeleteStoryState()
                val stories =
                    (storyState.userStoriesState as? Response.Success)?.data ?: emptyList()
                if (stories.isNotEmpty() && currentStoryIndex < stories.size) {
                    progress = 0f
                } else {
                    navController.popBackStack()
                }
            }

            is Response.Error -> {
                storyViewModel.resetDeleteStoryState()
            }

            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val storiesResponse = storyState.userStoriesState) {
            is Response.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }

            is Response.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = storiesResponse.message,
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            is Response.Success -> {
                val user = if (isCurrentUser) {
                    userState.currentUserState.getDataOrNull()
                } else {
                    userState.otherUserState.getDataOrNull()
                }

                val stories = storiesResponse.data.map { story ->
                    story.copy(
                        userName = user?.username ?: "",
                        userProfilePicture = user?.imageUrl ?: ""
                    )
                }
                if (stories.isNotEmpty() && currentStoryIndex < stories.size) {
                    StoryViewerContent(
                        stories = stories,
                        currentIndex = currentStoryIndex,
                        progress = progress,
                        isPaused = isPaused,
                        isCurrentUser = isCurrentUser,
                        onPause = { isPaused = !isPaused },
                        onNext = {
                            if (currentStoryIndex < stories.size - 1) {
                                currentStoryIndex++
                                progress = 0f
                            } else {
                                navController.popBackStack()
                            }
                        },
                        onPrevious = {
                            if (currentStoryIndex > 0) {
                                currentStoryIndex--
                                progress = 0f
                            }
                        },
                        onClose = { navController.popBackStack() },
                        onDelete = {
                            isPaused = true
                            showDeleteDialog = true
                        }
                    )
                } else {
                    navController.popBackStack()
                }
                if (showDeleteDialog && stories.isNotEmpty() && currentStoryIndex < stories.size) {
                    Dialog(
                        title = "Delete Story",
                        message = "Are you sure you want to delete this story?",
                        confirmText = "Delete",
                        dismissText = "Cancel",
                        onConfirm = {
                            showDeleteDialog = false
                            val currentStory = stories[currentStoryIndex]
                            storyViewModel.deleteStory(currentStory.storyID, userId)
                        },
                        onDismiss = {
                            showDeleteDialog = false
                            isPaused = false
                        }
                    )
                }
            }

            is Response.Idle -> {}
        }


    }
}

@Composable
private fun StoryViewerContent(
    stories: List<Story>,
    currentIndex: Int,
    progress: Float,
    isPaused: Boolean,
    isCurrentUser: Boolean,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    Log.d("StoryViewerContent", "Stories: $stories, Current Index: $currentIndex")
    val currentStory = stories[currentIndex]
    var isMediaReady by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableFloatStateOf(0f) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentIndex, isPaused, videoViewRef) {
        if (currentStory.mediaType == "video" && videoViewRef != null && !isPaused) {
            coroutineScope.launch {
                while (videoViewRef?.isPlaying == true) {
                    val currentPosition = videoViewRef?.currentPosition ?: 0
                    val duration = videoViewRef?.duration ?: 1
                    if (duration > 0) {
                        videoProgress = currentPosition.toFloat() / duration
                    }
                    if (videoProgress >= 1f) {
                        onNext()
                        break
                    }
                    delay(50L)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                onClick = onPause,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        if (!isMediaReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }

        when (currentStory.mediaType) {
            "video" -> {
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setVideoPath(currentStory.mediaUrl)
                            setOnPreparedListener { mediaPlayer ->
                                mediaPlayer.isLooping = false
                                isMediaReady = true
                                videoViewRef = this
                                if (!isPaused) start()
                            }
                            setOnErrorListener { _, _, _ ->
                                isMediaReady = false
                                true
                            }
                            setOnCompletionListener {
                                onNext()
                            }
                        }
                    },
                    update = { videoView ->
                        if (isPaused) {
                            videoView.pause()
                        } else {
                            videoView.start()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentStory.mediaUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.placehoder_image)
                        .error(R.drawable.placehoder_image)
                        .build(),
                    contentDescription = "Story Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onSuccess = { isMediaReady = true },
                    onError = { isMediaReady = false },
                    onLoading = { isMediaReady = false }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Progress bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { index, _ ->
                    LinearProgressIndicator(
                        progress = {
                            when {
                                index < currentIndex -> 1f
                                index == currentIndex -> if (currentStory.mediaType == "video") videoProgress else progress
                                else -> 0f
                            }
                        },
                        modifier = Modifier
                            .weight(1F)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                }
            }

            // Top bar with user info and buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentStory.userProfilePicture)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = currentStory.userName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentStory.timestamp.toTimeAgo(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                if (isCurrentUser) {
                    IconButton(
                        onClick = onDelete
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Story",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { /* Handle more options */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Navigation areas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 64.dp) // Add padding to avoid overlap with top bar
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.3f)
                        .clickable(
                            onClick = onPrevious,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.4f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.3f)
                        .clickable(
                            onClick = onNext,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                )
            }
        }
    }
}