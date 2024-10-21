package com.arny.allfy.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.arny.allfy.R
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.BottomNavigationItem
import com.arny.allfy.presentation.common.BottomNavigation
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screens

@Composable
fun ProfileScreen(navController: NavController) {
    val userViewModel: UserViewModel = hiltViewModel()
    userViewModel.getUserInfo()
    when (val response = userViewModel.getUserData.value) {
        is Response.Loading -> {
            CircularProgressIndicator()
        }

        is Response.Success -> {
            val user = response.data
            Scaffold(
                bottomBar = { BottomNavigation(BottomNavigationItem.Profile, navController) }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
//                            .padding(16.dp)
                ) {
                    ProfileScreen(navController, user)
                }
            }
        }

        is Response.Error -> {}
    }
    Column(modifier = Modifier.fillMaxSize()) {
//        Button(onClick = {
//            authViewModel.signOut()
//            navController.navigate(Screens.LoginScreen.route) {
//                popUpTo(Screens.FeedScreen.route) {
//                    inclusive = true
//                }
//            }
//        }) { }
//        Column(modifier = Modifier.weight(2f)) {
//            Text(text = "Profile Screen")
//        }
    }
}

@Preview
@Composable
fun ProfileScreenPreview() {
    val navController = NavController(LocalContext.current)
    ProfileScreen(user = User(), navController = navController)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, user: User) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screens.CreatePostScreen.route)
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New Post")
                    }
                    IconButton(onClick = {
                        // Navigate to settings screen
                        navController.navigate(Screens.SettingsScreen.route)
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Profile Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(user.imageUrl),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = user.userName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.bio,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatisticItem("Posts", user.totalPosts)
                StatisticItem("Followers", user.followers.size.toString())
                StatisticItem("Following", user.following.size.toString())
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit Profile Button
            OutlinedButton(
                onClick = {
                    navController.navigate(Screens.EditProfileScreen.route)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Profile")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Photo Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(30) { index ->
                    Image(
//                        painter = painterResource(id = R.drawable.placehoder_image),
                        painter = rememberAsyncImagePainter(
                            user.imageUrl,
                            placeholder = rememberAsyncImagePainter(R.drawable.placehoder_image)
                        ),
                        contentDescription = "Post $index",
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun StatisticItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 14.sp
        )
    }
}
