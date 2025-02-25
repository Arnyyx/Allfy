package com.arny.allfy.presentation.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.formatTimestamp

@Composable
fun ConversationsScreen(
    navHostController: NavHostController,
    userViewModel: UserViewModel,
    chatViewModel: ChatViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val followersState by userViewModel.followers.collectAsState()

    val currentUserViewModel by userViewModel.currentUser.collectAsState()
    val conversationsState by chatViewModel.loadConversationsState.collectAsState()
    var currentUser by remember { mutableStateOf(User()) }

    when (currentUserViewModel) {
        is Response.Loading -> LoadingIndicator()
        is Response.Error -> ErrorMessage((currentUserViewModel as Response.Error).message)
        is Response.Success -> {
            currentUser = (currentUserViewModel as Response.Success<User>).data
            LaunchedEffect(currentUser) {
                chatViewModel.loadConversations(currentUser.userID)
            }
        }
    }

    when (conversationsState) {
        is Response.Success -> {
            val conversations = (conversationsState as Response.Success<List<Conversation>>).data
            val otherUserIDs = mutableSetOf<String>()
            conversations.forEach { otherUserIDs.add(it.otherUserID) }
            LaunchedEffect(otherUserIDs) {
                userViewModel.getUsers(otherUserIDs.toList())
            }
        }

        else -> {}
    }
    val usersState by userViewModel.users.collectAsState()
    val userMap = createUserMap(usersState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MessagesTopBar(navHostController)
        SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })

        // Load followers if there are any
        LaunchedEffect(currentUser.followers) {
            if (currentUser.followers.isNotEmpty()) {
                userViewModel.getFollowers(currentUser.followers)
            }
        }

        when (followersState) {
            is Response.Loading -> {
                if (currentUser.followers.isNotEmpty()) {
                    LoadingIndicator()
                } else {
                    // If no followers, show conversations directly
                    ConversationsSection(navHostController, chatViewModel, userMap, currentUser)
                }
            }

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

private fun createUserMap(usersState: Response<List<User>>): Map<String, User> {
    return when (usersState) {
        is Response.Success -> {
            usersState.data.associateBy { it.userID }
        }

        else -> emptyMap()
    }
}

/**
 * UI Components - Top Section
 */
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
            focusedContainerColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

/**
 * UI Components - Followers Section
 */
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
    currentUser: User,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clickable {
                navHostController.navigate("chat/${currentUser.userID}/${follower.userID}")
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

            if (follower.isOnline) {
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
            text = follower.userName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * UI Components - Conversations Section
 */
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
                        ConversationItem(
                            conversation = conversation,
                            userMap = userMap,
                            onClick = {
                                navHostController.navigate("chat/${currentUser.userID}/${conversation.otherUserID}")
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = userMap[conversation.otherUserID]?.imageUrl ?: "",
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .fillMaxSize()
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
                    text = userMap[conversation.otherUserID]?.userName ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = formatTimestamp(
                        conversation.lastMessage?.timestamp ?: conversation.timestamp
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = conversation.lastMessage?.content ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(text = conversation.unreadCount.toString())
                    }
                }
            }
        }
    }
}

/**
 * Common UI Components
 */
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