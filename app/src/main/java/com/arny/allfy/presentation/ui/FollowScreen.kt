package com.arny.allfy.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.utils.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    userId: String,
    initialTab: Int = 0
) {
    val userState by userViewModel.userState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(initialTab) }

    LaunchedEffect(userId) {
        userViewModel.getFollowers(userId)
        userViewModel.getFollowings(userId)
        userViewModel.getFollowingCount(userId)
        userViewModel.getFollowersCount(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val username = when (val userResponse = userState.currentUserState) {
                        is Response.Success -> userResponse.data.username
                        else -> ""
                    }
                    Text(
                        text = username,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Followers (${
                                (userState.followersCountState as? Response.Success)?.data ?: 0
                            })"
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Following (${
                                (userState.followingCountState as? Response.Success)?.data ?: 0
                            })"
                        )
                    }
                )
            }

            when (selectedTabIndex) {
                0 -> FollowersList(userViewModel, navController)
                1 -> FollowingList(userViewModel, navController)
            }
        }
    }
}

@Composable
fun FollowersList(
    userViewModel: UserViewModel,
    navController: NavController
) {
    val userState by userViewModel.userState.collectAsState()

    when (val followersState = userState.followersState) {
        is Response.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator()
            }
        }

        is Response.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = followersState.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        is Response.Success -> {
            val followers = followersState.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(followers) { user ->
                    UserItem(user, navController)
                }
            }
        }

        is Response.Idle -> {}
    }
}

@Composable
fun FollowingList(
    userViewModel: UserViewModel,
    navController: NavController
) {
    val userState by userViewModel.userState.collectAsState()

    when (val followingsState = userState.followingsState) {
        is Response.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator()
            }
        }

        is Response.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = followingsState.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        is Response.Success -> {
            val followings = followingsState.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(followings) { user ->
                    UserItem(user, navController)
                }
            }
        }

        is Response.Idle -> {}
    }
}

@Composable
fun UserItem(user: User, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Screen.ProfileScreen(user.userId)) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(user.imageUrl.ifEmpty { null })
                .crossfade(true)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .build(),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = user.username,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            user.name.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}