package com.arny.allfy.presentation.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.Conversation
import com.arny.allfy.domain.model.MessageType
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Screen
import com.arny.allfy.utils.formatTimestamp
import com.arny.allfy.utils.getDataOrNull
import com.arny.allfy.utils.isLoading
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    navHostController: NavHostController,
    userViewModel: UserViewModel,
    chatViewModel: ChatViewModel
) {
    val userState by userViewModel.userState.collectAsState()
    val chatState by chatViewModel.chatState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val currentUser = userState.currentUserState.getDataOrNull()
    val users = userState.usersState.getDataOrNull() ?: emptyList()
    val followers = userState.followersState.getDataOrNull() ?: emptyList()
    val conversations = chatState.loadConversationsState.getDataOrNull() ?: emptyList()
    val userMap = users.associateBy { it.userId }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            chatViewModel.loadConversations(user.userId)
            userViewModel.getFollowers(user.userId)
        }
    }

    LaunchedEffect(conversations) {
        if (conversations.isNotEmpty() && currentUser != null) {
            val participantIds =
                conversations.flatMap { it.participants }.toSet() - currentUser.userId
            if (participantIds.isNotEmpty()) {
                userViewModel.getUsersByIDs(participantIds.toList())
            }
        }
    }

    val isLoading = chatState.loadConversationsState.isLoading ||
            userState.usersState.isLoading ||
            userState.followersState.isLoading

    Scaffold(
        topBar = {
            Column {
                MessagesTopBar(navHostController = navHostController)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
            FollowersSection(
                followers = followers,
                currentUser = currentUser ?: User(),
                navHostController = navHostController,
            )
            ConversationsSection(
                conversations = conversations,
                navHostController = navHostController,
                userMap = userMap,
                currentUser = currentUser ?: User(),
                searchQuery = searchQuery
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessagesTopBar(navHostController: NavHostController) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Messages",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row {
                    IconButton(onClick = { /* TODO: Handle video call */ }) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Video Call",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { /* TODO: Handle new message */ }) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "New Message",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search conversations") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp),
        singleLine = true
    )
}

@Composable
private fun FollowersSection(
    followers: List<User>,
    navHostController: NavHostController,
    currentUser: User,
) {
    if (followers.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Followers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            val listState = rememberLazyListState()
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(followers, key = { it.userId }) { follower ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
                    ) {
                        FollowerItem(
                            follower = follower,
                            navController = navHostController,
                            currentUser = currentUser
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowerItem(
    follower: User,
    navController: NavHostController,
    currentUser: User
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clickable {
                navController.navigate(
                    Screen.ChatScreen(
                        otherUserId = follower.userId
                    )
                )
            }
        ) {
            AsyncImageWithPlaceholder(
                imageUrl = follower.imageUrl,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
            )
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
private fun AsyncImageWithPlaceholder(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .placeholder(R.drawable.ic_user) // Placeholder khi đang load
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
private fun ConversationsSection(
    conversations: List<Conversation>,
    navHostController: NavHostController,
    userMap: Map<String, User>,
    currentUser: User,
    searchQuery: String
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val conversationss = conversations.filter {
        val otherUserId =
            it.participants.firstOrNull { id -> id != currentUser.userId } ?: ""
        val otherUser = userMap[otherUserId]
        otherUser?.username?.contains(searchQuery, ignoreCase = true) ?: true
    }
    if (conversationss.isEmpty()) {
        EmptyState("No conversations found")
    } else {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = conversationss,
                key = { it.id }
            ) { conversation ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
                ) {
                    val otherUserId =
                        conversation.participants.firstOrNull { it != currentUser.userId }
                            ?: ""
                    ConversationItem(
                        conversation = conversation,
                        userMap = userMap,
                        currentUserId = currentUser.userId,
                        onClick = {
                            navHostController.navigate(
                                Screen.ChatScreen(
                                    conversationId = conversation.id,
                                    otherUserId = otherUserId
                                )
                            )
                        }
                    )
                }
            }
        }
        LaunchedEffect(conversationss.size) {
            if (conversationss.isNotEmpty() && listState.firstVisibleItemIndex <= 1) {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImageWithPlaceholder(
            imageUrl = otherUser?.imageUrl ?: "",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
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
                    text = otherUser?.username ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTimestamp(lastMessage?.timestamp ?: conversation.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = when (lastMessage?.type) {
                        MessageType.IMAGE -> if (lastMessage.senderId == currentUserId) "You sent an image" else "${otherUser?.username ?: ""} sent an image"
                        MessageType.VIDEO -> if (lastMessage.senderId == currentUserId) "You sent a video" else "${otherUser?.username ?: ""} sent a video"
                        MessageType.FILE -> if (lastMessage.senderId == currentUserId) "You sent a file" else "${otherUser?.username ?: ""} sent a file"
                        MessageType.VOICE -> if (lastMessage.senderId == currentUserId) "You sent a voice message" else "${otherUser?.username ?: ""} sent a voice message"
                        MessageType.VOICE_CALL -> if (lastMessage.senderId == currentUserId) "You sent a voice call" else "${otherUser?.username ?: ""} sent a voice call"
                        MessageType.VIDEO_CALL -> if (lastMessage.senderId == currentUserId) "You sent a video call" else "${otherUser?.username ?: ""} sent a video call"
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
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
