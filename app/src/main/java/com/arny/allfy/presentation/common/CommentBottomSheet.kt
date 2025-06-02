package com.arny.allfy.presentation.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.Comment
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.state.PostState
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.utils.Response
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    postId: String,
    currentUser: User,
    isVisible: Boolean,
    postViewModel: PostViewModel,
    onDismiss: () -> Unit
) {
    val postState by postViewModel.postState.collectAsState()
    val commentText = remember { mutableStateOf("") }
    val replyingToCommentId = remember { mutableStateOf<String?>(null) }
    val selectedImageUri = remember { mutableStateOf<Uri?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri.value = uri
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            postViewModel.loadComments(postId, reset = true)
        }
    }

    LaunchedEffect(postState.addCommentState) {
        when (postState.addCommentState) {
            is Response.Success -> {
                postViewModel.loadComments(postId, reset = true)
                commentText.value = ""
                replyingToCommentId.value = null
                selectedImageUri.value = null
                keyboardController?.hide()
                postViewModel.resetAddCommentState()
            }

            else -> {}
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (val commentsResponse = postState.loadCommentsState) {
                            is Response.Success -> "${commentsResponse.data.size} comments"
                            else -> "0 comments"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CommentsList(
                        postId = postId,
                        currentUserId = currentUser.userId,
                        postViewModel = postViewModel,
                        onReplyClick = { commentId ->
                            replyingToCommentId.value = commentId
                        },
                        postState = postState
                    )
                }
                CommentInput(
                    currentUser = currentUser,
                    commentText = commentText.value,
                    onCommentTextChange = { commentText.value = it },
                    onSendComment = {
                        if (commentText.value.isNotBlank() || selectedImageUri.value != null) {
                            postViewModel.addComment(
                                postID = postId,
                                userID = currentUser.userId,
                                content = commentText.value,
                                parentCommentID = replyingToCommentId.value,
                                imageUri = selectedImageUri.value
                            )
                        }
                    },
                    isSending = postState.addCommentState is Response.Loading,
                    replyingTo = replyingToCommentId.value?.let { id ->
                        (postState.loadCommentsState as? Response.Success)?.data?.find {
                            it.commentID == id
                        }?.commentOwnerUserName
                    },
                    selectedImageUri = selectedImageUri.value,
                    onImagePick = { imagePickerLauncher.launch("image/*") },
                    onRemoveImage = { selectedImageUri.value = null },
                    onCancelReply = { replyingToCommentId.value = null }
                )
            }
        }
    }
}

@Composable
private fun CommentsList(
    postId: String,
    postState: PostState,
    postViewModel: PostViewModel,
    currentUserId: String,
    onReplyClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    when (val commentsResponse = postState.loadCommentsState) {
        is Response.Loading -> LoadingState()
        is Response.Error -> ErrorState(commentsResponse.message)
        is Response.Success -> {
            val comments = commentsResponse.data
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
                    ) { _, comment ->
                        val replies = comments.filter { it.parentCommentID == comment.commentID }
                        CommentItem(
                            postId = postId,
                            comment = comment,
                            replies = replies,
                            postViewModel = postViewModel,
                            currentUserId = currentUserId,
                            onReplyClick = onReplyClick
                        )
                    }
                    if (commentsResponse.hasMore) {
                        item {
                            LaunchedEffect(Unit) {
                                scope.launch {
                                    postViewModel.loadComments(postId, reset = false)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        is Response.Idle -> {}
    }
}

@Composable
private fun CommentItem(
    postId: String,
    comment: Comment,
    replies: List<Comment>,
    postViewModel: PostViewModel,
    currentUserId: String,
    onReplyClick: (String) -> Unit
) {
    var showReplies by remember { mutableStateOf(false) }
    val isLiked by remember(comment) { derivedStateOf { comment.likes.contains(currentUserId) } }
    val likeCount by remember(comment) { derivedStateOf { comment.likes.size } }
    val isLiking by remember(postViewModel.loadingPosts.collectAsState().value) {
        derivedStateOf {
            postViewModel.loadingPosts.value[postId]?.contains("likeComment") ?: false
        }
    }

    Column {
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
                if (comment.content.isNotBlank()) {
                    Text(
                        text = comment.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                comment.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.placehoder_image)
                            .error(R.drawable.placehoder_image)
                            .build(),
                        contentDescription = "Comment Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .padding(top = 4.dp),
                        contentScale = ContentScale.Inside,
                        onSuccess = { state ->
                            val drawable = state.result.drawable
                            if (drawable != null) {
                                val aspectRatio =
                                    drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
                                // Update modifier dynamically if needed, but for simplicity, we use fixed sizes
                            }
                        }
                    )
                }
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
                                    postID = postId,
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
                        if (likeCount > 0) {
                            Text(
                                text = likeCount.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLiked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        if (isLiking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    Text(
                        text = "Reply",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { onReplyClick(comment.commentID) }
                    )
                    if (replies.isNotEmpty()) {
                        Text(
                            text = if (showReplies) "Hide Replies" else "Show ${replies.size} Replies",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showReplies = !showReplies }
                        )
                    }
                }
            }
        }
        if (showReplies && replies.isNotEmpty()) {
            replies.forEach { reply ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImageWithPlaceholder(
                        imageUrl = reply.commentOwnerProfilePicture,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = reply.commentOwnerUserName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (reply.content.isNotBlank()) {
                            Text(
                                text = reply.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        reply.imageUrl?.let { imageUrl ->
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .placeholder(R.drawable.placehoder_image)
                                    .error(R.drawable.placehoder_image)
                                    .build(),
                                contentDescription = "Reply Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(top = 4.dp),
                                contentScale = ContentScale.Fit,
                                onSuccess = { state ->
                                    val drawable = state.result.drawable
                                    if (drawable != null) {
                                        val aspectRatio =
                                            drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
                                        // Update modifier dynamically if needed
                                    }
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = reply.timeAgo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable(
                                    enabled = !isLiking,
                                    onClick = {
                                        postViewModel.toggleLikeComment(
                                            postID = postId,
                                            comment = reply,
                                            userID = currentUserId
                                        )
                                    }
                                )
                            ) {
                                Icon(
                                    imageVector = if (reply.likes.contains(currentUserId)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (reply.likes.contains(currentUserId)) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                if (reply.likes.size > 0) {
                                    Text(
                                        text = reply.likes.size.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (reply.likes.contains(currentUserId)) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
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
    replyingTo: String?,
    selectedImageUri: Uri?,
    onImagePick: () -> Unit,
    onRemoveImage: () -> Unit,
    onCancelReply: () -> Unit
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Replying to $replyingTo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    IconButton(
                        onClick = onCancelReply,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Reply",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            selectedImageUri?.let { uri ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(uri)
                        .crossfade(true)
                        .placeholder(R.drawable.placehoder_image)
                        .error(R.drawable.placehoder_image)
                        .build(),
                    contentDescription = "Selected Comment Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(bottom = 4.dp),
                    contentScale = ContentScale.Fit,
                    onSuccess = { state ->
                        val drawable = state.result.drawable
                        if (drawable != null) {
                            val aspectRatio =
                                drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
                        }
                    }
                )
                IconButton(
                    onClick = onRemoveImage,
                    modifier = Modifier
                        .align(Alignment.End)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Image",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
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
                IconButton(
                    onClick = onImagePick,
                    enabled = !isSending && selectedImageUri == null
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Attach Image",
                        tint = if (selectedImageUri == null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(
                    onClick = onSendComment,
                    enabled = !isSending && (commentText.isNotBlank() || selectedImageUri != null)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send comment",
                            tint = if (commentText.isNotBlank() || selectedImageUri != null) MaterialTheme.colorScheme.primary
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
            .data(imageUrl.ifEmpty { null })
            .crossfade(true)
            .placeholder(R.drawable.ic_user)
            .error(R.drawable.ic_user)
            .build()
    )
    Image(
        painter = painter,
        contentDescription = "User Avatar",
        modifier = modifier,
        contentScale = ContentScale.Crop
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