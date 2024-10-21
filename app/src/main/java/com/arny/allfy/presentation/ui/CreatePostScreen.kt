// CreatePostScreen.kt
package com.arny.allfy.presentation.ui

import android.net.Uri
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    onBackClick: () -> Unit,
    postViewModel: PostViewModel = hiltViewModel(),
) {

    val userViewModel: UserViewModel = hiltViewModel()
    userViewModel.getUserInfo()
    var user = User()
    when (val response = userViewModel.getUserData.value) {
        is Response.Loading -> {}
        is Response.Success -> {
            user = response.data
        }

        is Response.Error -> {}
    }

    var captionText by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

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
        // Top Bar
        TopAppBar(
            title = { Text("New Post") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        if (selectedImageUris.isNotEmpty()) {
                            val post = Post(
                                userID = user.userID,
                                username = user.userName,
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

                // Page count
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

        // Caption Input
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

        // Action Buttons
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

        when (val response = postViewModel.uploadPostSate.value) {
            is Response.Loading -> {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            is Response.Success -> {
                LaunchedEffect(response.data) {
                    if (response.data) {
                        onBackClick()
                    }
                }
            }

            is Response.Error -> {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = response.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(start = 16.dp, end = 16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCreatePostScreen() {
    CreatePostScreen(
        onBackClick = {}
    )
}
