package com.arny.allfy.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.Comment
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.utils.Response
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    post: Post,
    currentUser: User,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    postViewModel: PostViewModel = hiltViewModel()
) {
    val commentText = remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(isVisible) {
        if (isVisible) {
            postViewModel.loadComments(post.postID)
        }
    }

    LaunchedEffect(postViewModel.addCommentState.value) {
        if (postViewModel.addCommentState.value is Response.Success) {
            postViewModel.loadComments(post.postID)
            commentText.value = ""
            keyboardController?.hide()
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 50.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Comments",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Sử dụng Column thay vì Box để bố trí CommentsList và CommentInput
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // CommentsList chiếm phần không gian linh hoạt
                ) {
                    CommentsList(postViewModel, post.postID)
                }
                CommentInput(
                    currentUser = currentUser,
                    commentText = commentText.value,
                    onCommentTextChange = { commentText.value = it },
                    onSendComment = {
                        if (commentText.value.isNotBlank()) {
                            postViewModel.addComment(
                                postID = post.postID,
                                userID = currentUser.userId,
                                content = commentText.value
                            )
                        }
                    },
                    isSending = postViewModel.addCommentState.value is Response.Loading
                )
            }
        }
    }
}

@Composable
private fun CommentsList(
    postViewModel: PostViewModel,
    postId: String
) {
    val commentsResponse by postViewModel.comments.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    when (commentsResponse) {
        is Response.Loading -> LoadingState()
        is Response.Success -> {
            val comments = (commentsResponse as Response.Success<List<Comment>>).data
            if (comments.isEmpty()) {
                EmptyState("No comments yet")
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(
                        items = comments,
                        key = { _, comment -> comment.commentID }
                    ) { index, comment ->
                        AnimatedCommentItem(comment = comment, delayIndex = index)
                    }
                }
                LaunchedEffect(comments.size) {
                    if (comments.isNotEmpty() && listState.firstVisibleItemIndex <= 1) {
                        scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                }
            }
        }
        is Response.Error -> ErrorState("Failed to load comments")
    }
}

@Composable
private fun AnimatedCommentItem(comment: Comment, delayIndex: Int) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayIndex * 100L)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)) + expandVertically(tween(300))
    ) {
        CommentItem(comment)
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImageWithPlaceholder(
            imageUrl = comment.commentOwnerProfilePicture,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = comment.commentOwnerUserName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = comment.timeAgo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Like",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { /* TODO: Handle like */ }
                )
                Text(
                    text = "Reply",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.clickable { /* TODO: Handle reply */ }
                )
            }
        }
    }
}

@Composable
private fun CommentInput(
    currentUser: User,
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onSendComment: () -> Unit,
    isSending: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsyncImageWithPlaceholder(
                imageUrl = currentUser.imageUrl,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
            OutlinedTextField(
                value = commentText,
                onValueChange = onCommentTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a comment...") },
                maxLines = 5,
                enabled = !isSending,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    IconButton(
                        onClick = onSendComment,
                        enabled = commentText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send comment",
                            tint = if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AsyncImageWithPlaceholder(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(if (imageUrl.isNotEmpty()) imageUrl else null)
            .crossfade(true)
            .placeholder(R.drawable.ic_user)
            .error(R.drawable.ic_user)
            .build()
    )
    Image(
        painter = painter,
        contentDescription = "User Avatar",
        modifier = modifier,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop
    )
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LinearProgressIndicator()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Loading comments...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}