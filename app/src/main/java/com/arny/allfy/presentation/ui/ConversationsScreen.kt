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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.TextFieldColors
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
import com.arny.allfy.presentation.common.Toast
import com.arny.allfy.presentation.state.ConversationsUiState
import com.arny.allfy.presentation.viewmodel.ConversationsViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.formatTimestamp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

@Composable
fun FollowersSection(followers: List<User>, navHostController: NavHostController) {
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
                FollowerItem(
                    follower = follower,
                    navHostController = navHostController
                )
            }
        }
    }
}

@Composable
fun FollowerItem(follower: User, navHostController: NavHostController) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clickable(
                onClick = {
                    navHostController.navigate("chat/${follower.userID}")
                }
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesTopBar(
    navHostController: NavHostController
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        navHostController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Text(
                        text = "arny cute hehehe",
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

@OptIn(ExperimentalMaterial3Api::class)
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

@Composable
private fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar
        AsyncImage(
            model = conversation.participantAvatar,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .fillMaxSize()
                .clip(CircleShape)
        )

        // Content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = conversation.participantName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = formatTimestamp(conversation.timestamp),
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

@Composable
fun ConversationsScreen(
    navHostController: NavHostController,
    conversationsViewModel: ConversationsViewModel,
    userViewModel: UserViewModel,
    onConversationClick: (String) -> Unit
) {
    val uiState by conversationsViewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val currentUser by userViewModel.currentUser.collectAsState()
    val followersState by userViewModel.followers.collectAsState()

    LaunchedEffect(Unit) {
        userViewModel.getCurrentUser()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MessagesTopBar(navHostController)
        SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })

        when (currentUser) {
            is Response.Loading -> {
                LoadingIndicator()
            }

            is Response.Success -> {
                val user = (currentUser as Response.Success<User>).data
                userViewModel.getFollowers(user.followers)

            }

            is Response.Error -> ErrorMessage((currentUser as Response.Error).message)
        }
        when (followersState) {
            is Response.Loading -> {
                LoadingIndicator()
            }

            is Response.Success -> {
                val followers = (followersState as Response.Success<List<User>>).data
                FollowersSection(followers, navHostController)
                ConversationsSection(uiState, onConversationClick)
            }

            is Response.Error -> ErrorMessage((followersState as Response.Error).message)
        }
    }
}

@Composable
private fun ConversationsSection(
    uiState: ConversationsUiState,
    onConversationClick: (String) -> Unit
) {
    when (uiState) {
        is ConversationsUiState.Initial -> LoadingIndicator()
        is ConversationsUiState.Success -> {
            if (uiState.conversations.isEmpty()) {
                EmptyState("No conversations found")
            } else {
                LazyColumn {
                    items(uiState.conversations) { conversation ->
                        ConversationItem(conversation, onClick = { })
                    }
                }
            }
        }

        is ConversationsUiState.Error -> ErrorMessage(uiState.message)
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