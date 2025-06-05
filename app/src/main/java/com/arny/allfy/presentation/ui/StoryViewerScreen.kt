package com.arny.allfy.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.Story
import com.arny.allfy.presentation.viewmodel.StoryViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.getDataOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoryViewerScreen(
    navController: NavController,
    storyViewModel: StoryViewModel,
    userViewModel: UserViewModel,
    userId: String
) {
    val storyState by storyViewModel.storyState.collectAsState()
    var currentStoryIndex by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        storyViewModel.getUserStories(userId)
    }

    LaunchedEffect(currentStoryIndex) {
        val stories = (storyState.userStoriesState as? Response.Success)?.data ?: emptyList()
        if (stories.isNotEmpty() && currentStoryIndex < stories.size) {
            val currentUserId =
                userViewModel.userState.value.currentUserState.getDataOrNull()?.userId ?: ""
            storyViewModel.logStoryView(currentUserId, stories[currentStoryIndex].storyID)
            coroutineScope.launch {
                delay(5000L)
                if (currentStoryIndex < stories.size - 1) {
                    currentStoryIndex++
                } else {
                    navController.popBackStack()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val storiesResponse = storyState.userStoriesState) {
            is Response.Loading -> {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            is Response.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = storiesResponse.message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 16.sp
                    )
                }
            }

            is Response.Success -> {
                val stories = storiesResponse.data
                if (stories.isNotEmpty() && currentStoryIndex < stories.size) {
                    StoryViewerContent(
                        story = stories[currentStoryIndex],
                        onNext = {
                            if (currentStoryIndex < stories.size - 1) {
                                currentStoryIndex++
                            } else {
                                navController.popBackStack()
                            }
                        },
                        onPrevious = {
                            if (currentStoryIndex > 0) {
                                currentStoryIndex--
                            }
                        }
                    )
                } else {
                    navController.popBackStack()
                }
            }

            is Response.Idle -> {}
        }
    }
}

@Composable
private fun StoryViewerContent(
    story: Story,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                onClick = { /* Ngăn chặn click xuyên qua */ },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        // Story Media
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(story.mediaUrl)
                .crossfade(true)
                .placeholder(R.drawable.placehoder_image)
                .error(R.drawable.placehoder_image)
                .build(),
            contentDescription = "Story Media",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Caption
        if (!story.caption.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Text(
                    text = story.caption,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Navigation Areas
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .clickable { onPrevious() }
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .align(Alignment.CenterEnd)
                .clickable { onNext() }
        )

        // Progress Indicator
        LinearProgressIndicator(
            progress = { 0.0f }, // Có thể thêm logic để hiển thị tiến độ
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.primary
        )
    }
}
