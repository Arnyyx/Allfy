package com.arny.allfy.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arny.allfy.domain.model.Comment
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.utils.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    post: Post,
    currentUser: User,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    postViewModel: PostViewModel = hiltViewModel()
) {
    val comments by postViewModel.comments.collectAsState()
    val commentText = remember { mutableStateOf("") }
    postViewModel.loadComments(post.postID)

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Comments",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Comments list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(comments) { comment ->
                        CommentItem(comment)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Comment input section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
//                    AsyncImage(
////                        model = currentUser.imageUrl,
//                        model = painterResource(R.drawable.ic_user),
//                        contentDescription = "User Avatar",
//                        placeholder = painterResource(R.drawable.ic_user),
//                        modifier = Modifier
//                            .size(32.dp)
//                            .clip(CircleShape)
//                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = commentText.value,
                        onValueChange = { commentText.value = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Add a comment...") },
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )



                    when (postViewModel.addCommentState.value) {
                        is Response.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterVertically))
                        }

                        is Response.Success -> {
                            IconButton(
                                onClick = {
                                    if (commentText.value.isNotBlank()) {
                                        postViewModel.addComment(
                                            postID = post.postID,
                                            userID = currentUser.userID,
                                            content = commentText.value
                                        )
                                        commentText.value = ""
                                    }
                                },
                                enabled = commentText.value.isNotBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send comment",
                                    tint = if (commentText.value.isNotBlank())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        is Response.Error -> {
                            Text(text = "Error")
                        }
                    }
                }

                // Add extra padding at the bottom to account for keyboard
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
//        AsyncImage(
////            model = comment.userProfilePicture,
//            model = painterResource(R.drawable.ic_user),
//            contentDescription = "Commenter Avatar",
//            placeholder = painterResource(R.drawable.ic_user),
//            modifier = Modifier
//                .size(32.dp)
//                .clip(CircleShape)
//        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = comment.commentOwnerUserName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
//                Text(
//                    text = comment.timeAgo,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
                Text(
                    text = "Like",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { /* Handle like */ }
                )
                Text(
                    text = "Reply",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { /* Handle reply */ }
                )
            }
        }
    }
}