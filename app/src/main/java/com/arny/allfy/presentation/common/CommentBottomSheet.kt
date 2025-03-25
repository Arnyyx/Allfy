package com.arny.allfy.presentation.common

import android.util.Log
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
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
    val replyingToCommentId = remember { mutableStateOf<String?>(null) }
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
            replyingToCommentId.value = null
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CommentsList(
                        post = post,
                        postViewModel = postViewModel,
                        currentUserId = currentUser.userId,
                        onReplyClick = { commentId ->
                            replyingToCommentId.value = commentId
                        }
                    )
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
                                content = commentText.value,
                                parentCommentID = replyingToCommentId.value
                            )
                        }
                    },
                    isSending = postViewModel.addCommentState.value is Response.Loading,
                    replyingTo = replyingToCommentId.value?.let { id ->
                        (postViewModel.comments.value as? Response.Success)?.data?.find { it.commentID == id }?.commentOwnerUserName
                    }
                )
            }
        }
    }
}

@Composable
private fun CommentsList(
    post: Post,
    postViewModel: PostViewModel,
    currentUserId: String,
    onReplyClick: (String) -> Unit
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
                val rootComments = comments.filter { it.parentCommentID == null }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(
                        items = rootComments,
                        key = { _, comment -> comment.commentID }
                    ) { index, comment ->
                        val replies = comments.filter { it.parentCommentID == comment.commentID }
                        AnimatedCommentItem(
                            post = post,
                            comment = comment,
                            replies = replies,
                            delayIndex = index,
                            postViewModel = postViewModel,
                            currentUserId = currentUserId,
                            onReplyClick = onReplyClick
                        )
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
private fun AnimatedCommentItem(
    post: Post,
    comment: Comment,
    replies: List<Comment>,
    delayIndex: Int,
    postViewModel: PostViewModel,
    currentUserId: String,
    onReplyClick: (String) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var showReplies by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayIndex * 100L)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)) + expandVertically(tween(300))
    ) {
        Column {
            CommentItem(
                post = post,
                comment = comment,
                postViewModel = postViewModel,
                currentUserId = currentUserId,
                onReplyClick = onReplyClick,
                onShowRepliesClick = { showReplies = !showReplies },
                hasReplies = replies.isNotEmpty(),
                repliesVisible = showReplies,
                repliesCount = replies.size
            )
            if (showReplies && replies.isNotEmpty()) {
                replies.forEach { reply ->
                    CommentItem(
                        post = post,
                        comment = reply,
                        postViewModel = postViewModel,
                        currentUserId = currentUserId,
                        onReplyClick = onReplyClick,
                        isReply = true,
                        onShowRepliesClick = {},
                        hasReplies = false, // Không cho phép Reply cấp 2
                        repliesVisible = false,
                        repliesCount = 0
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    post: Post,
    comment: Comment,
    postViewModel: PostViewModel,
    currentUserId: String,
    onReplyClick: (String) -> Unit,
    isReply: Boolean = false,
    onShowRepliesClick: () -> Unit,
    hasReplies: Boolean,
    repliesVisible: Boolean,
    repliesCount: Int = 0
) {
    val commentLikeLoadingStates by postViewModel.commentLikeLoadingStates.collectAsState()
    val isLiking = commentLikeLoadingStates[comment.commentID] == true
    val isLiked = comment.likes.contains(currentUserId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = if (isReply) 32.dp else 0.dp),
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable(
                        enabled = !isLiking,
                        onClick = {
                            postViewModel.toggleLikeComment(
                                postID = post.postID,
                                comment = comment,
                                userID = currentUserId
                            )
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (comment.likes.isNotEmpty()) comment.likes.size.toString() else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isLiked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    if (isLiking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                // Chỉ hiển thị nút "Reply" cho Comment gốc
                if (!isReply) {
                    Text(
                        text = "Reply",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { onReplyClick(comment.commentID) }
                    )
                }
                if (hasReplies) {
                    Text(
                        text = if (repliesVisible) "Hide Replies" else "Show $repliesCount Replies",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onShowRepliesClick() }
                    )
                }
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
    isSending: Boolean,
    replyingTo: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            if (replyingTo != null) {
                Text(
                    text = "Replying to $replyingTo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Row(
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
                    placeholder = { Text(if (replyingTo != null) "Write a reply..." else "Add a comment...") },
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