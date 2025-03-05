// CreatePostScreen.kt
package com.arny.allfy.presentation.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.arny.allfy.R
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    navHostController: NavHostController,
    postViewModel: PostViewModel,
    userViewModel: UserViewModel,
) {
    var captionText by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val currentUser by userViewModel.currentUser.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedImageUris = uris
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { selectedImageUris.size }
    )


    Column(modifier = Modifier.fillMaxSize()) {
        when (currentUser) {
            is Response.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is Response.Success -> {
                val user = (currentUser as Response.Success<User>).data
                // Only show create post UI when we have user data
                TopAppBar(
                    title = { Text("New Post") },
                    navigationIcon = {
                        IconButton(onClick = {
                            navHostController.popBackStack()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (selectedImageUris.isNotEmpty()) {
                                    val post = Post(
                                        postOwnerID = user.userId,
                                        postOwnerUsername = user.username,
                                        postOwnerImageUrl = user.imageUrl,
                                        caption = captionText
                                    )
                                    postViewModel.uploadPost(post, selectedImageUris)
                                }
                            },
                            enabled = selectedImageUris.isNotEmpty()
                        ) {
                            Text("Share", fontWeight = FontWeight.Bold)
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUris.isNotEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUris[page]),
                                contentDescription = "Selected Image ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        if (selectedImageUris.size > 1) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${selectedImageUris.size}",
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.placehoder_image),
                                contentDescription = "Placeholder Image",
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to select images")
                        }
                    }
                }

                TextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Write a caption...") },
                    maxLines = 5,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { /* TODO: Implement tagging people */ }) {
                        Icon(Icons.Default.Person, contentDescription = "Tag People")
                    }
                    IconButton(onClick = { /* TODO: Implement adding location */ }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Add Location")
                    }
                    IconButton(onClick = { /* TODO: Implement advanced settings */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                }

                // Handle post upload state
                when (val uploadResponse = postViewModel.uploadPostSate.value) {
                    is Response.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is Response.Success -> {
                        LaunchedEffect(uploadResponse.data) {
                            if (uploadResponse.data) {
                                navHostController.popBackStack()
                            }
                        }
                    }

                    is Response.Error -> {
                        Text(
                            text = uploadResponse.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            is Response.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = (currentUser as Response.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = {
                        userViewModel.getCurrentUser()
                    }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}