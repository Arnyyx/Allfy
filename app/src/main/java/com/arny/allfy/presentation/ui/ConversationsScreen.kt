package com.arny.allfy.presentation.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.MessageType
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.formatTimestamp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
@Composable
fun ConversationsScreen(
    navHostController: NavHostController,
    userViewModel: UserViewModel,
    chatViewModel: ChatViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val currentUserState by userViewModel.currentUser.collectAsState()
    val conversationsState by chatViewModel.loadConversationsState.collectAsState()
    var currentUser by remember { mutableStateOf(User()) }

    // Load current user
    when (currentUserState) {
        is Response.Loading -> LoadingIndicator()
        is Response.Error -> ErrorMessage((currentUserState as Response.Error).message)
        is Response.Success -> {
            currentUser = (currentUserState as Response.Success<User>).data
            LaunchedEffect(currentUser) {
                chatViewModel.loadConversations(currentUser.userId)
            }
        }
    }

    // Load conversations and users
    LaunchedEffect(conversationsState) {
        if (conversationsState is Response.Success) {
            val conversations = (conversationsState as Response.Success<List<Conversation>>).data
            val participantIds = conversations.flatMap { it.participants }.toSet() - currentUser.userId
            userViewModel.getUsers(participantIds.toList())
        }
    }
    val usersState by userViewModel.users.collectAsState()
    val userMap by remember(usersState) {
        derivedStateOf {
            when (usersState) {
                is Response.Success -> (usersState as Response.Success<List<User>>).data.associateBy { it.userId }
                else -> emptyMap()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MessagesTopBar(navHostController)
        SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })

        val followersState by userViewModel.followers.collectAsState()
        LaunchedEffect(currentUser.userId) {
            userViewModel.getFollowersFromSubcollection(currentUser.userId)
        }

        when (followersState) {
            is Response.Loading -> LoadingIndicator()
            is Response.Success -> {
                val followers = (followersState as Response.Success<List<User>>).data
                if (followers.isNotEmpty()) {
                    FollowersSection(followers, navHostController, currentUser, chatViewModel)
                }
                ConversationsSection(navHostController, chatViewModel, userMap, currentUser)
            }
            is Response.Error -> {
                ErrorMessage((followersState as Response.Error).message)
                ConversationsSection(navHostController, chatViewModel, userMap, currentUser)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesTopBar(navHostController: NavHostController) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Text(
                        text = "Messages",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    IconButton(onClick = { /* Handle video call */ }) {
                        Icon(Icons.Default.Call, "Video Call")
                    }
                    IconButton(onClick = { /* Handle new message */ }) {
                        Icon(Icons.Default.Create, "New Message")
                    }
                }
            }
        },
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    )
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Search") },
        leadingIcon = { Icon(Icons.Default.Search, "Search") },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

@Composable
fun FollowersSection(
    followers: List<User>,
    navHostController: NavHostController,
    currentUser: User,
    chatViewModel: ChatViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Followers",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(followers) { follower ->
                FollowerItem(follower, navHostController, currentUser)
            }
        }
    }
}

@Composable
fun FollowerItem(
    follower: User,
    navHostController: NavHostController,
    currentUser: User
) {
    val isOnlineState by getOnlineStatus(follower.userId).collectAsState(initial = false)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clickable {
                navHostController.navigate("chat/${currentUser.userId}/${follower.userId}")
            }
        ) {
            AsyncImage(
                model = follower.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            if (isOnlineState) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.Green)
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = follower.username,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ConversationsSection(
    navHostController: NavHostController,
    chatViewModel: ChatViewModel,
    userMap: Map<String, User>,
    currentUser: User
) {
    val conversationsState by chatViewModel.loadConversationsState.collectAsState()

    when (conversationsState) {
        is Response.Success -> {
            val conversations = (conversationsState as Response.Success<List<Conversation>>).data
            if (conversations.isEmpty()) {
                EmptyState("No conversations found")
            } else {
                LazyColumn {
                    items(
                        items = conversations,
                        key = { conversation -> conversation.id }
                    ) { conversation ->
                        val otherUserId = conversation.participants.firstOrNull { it != currentUser.userId } ?: ""
                        ConversationItem(
                            conversation = conversation,
                            userMap = userMap,
                            currentUserId = currentUser.userId,
                            onClick = {
                                navHostController.navigate("chat/${currentUser.userId}/$otherUserId")
                            }
                        )
                    }
                }
            }
        }
        is Response.Error -> ErrorMessage((conversationsState as Response.Error).message)
        Response.Loading -> LoadingIndicator()
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    userMap: Map<String, User>,
    currentUserId: String,
    onClick: () -> Unit
) {
    val otherUserId = conversation.participants.firstOrNull { it != currentUserId } ?: ""
    val otherUser = userMap[otherUserId]
    val lastMessage = conversation.lastMessage
    val unreadCountForCurrentUser = conversation.unreadCount[currentUserId] ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = otherUser?.imageUrl ?: "",
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = otherUser?.username ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTimestamp(lastMessage?.timestamp ?: conversation.timestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (lastMessage?.type) {
                        MessageType.IMAGE -> {
                            if (lastMessage.senderId == currentUserId) "Bạn đã gửi ảnh"
                            else "${otherUser?.username ?: ""} đã gửi ảnh"
                        }
                        MessageType.VIDEO -> {
                            if (lastMessage.senderId == currentUserId) "Bạn đã gửi video"
                            else "${otherUser?.username ?: ""} đã gửi video"
                        }
                        MessageType.FILE -> {
                            if (lastMessage.senderId == currentUserId) "Bạn đã gửi tệp"
                            else "${otherUser?.username ?: ""} đã gửi tệp"
                        }
                        else -> lastMessage?.content ?: ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (unreadCountForCurrentUser > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(text = unreadCountForCurrentUser.toString())
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp)
    )
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message)
    }
}

fun getOnlineStatus(userId: String): Flow<Boolean> = callbackFlow {
    val db = FirebaseDatabase.getInstance().reference.child("onlineStatus").child(userId)
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
            trySend(isOnline)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("RealtimeDB", "Error fetching online status: ${error.message}")
            trySend(false)
        }
    }
    db.addValueEventListener(listener)
    awaitClose { db.removeEventListener(listener) }
}