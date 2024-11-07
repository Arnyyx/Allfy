package com.arny.allfy.presentation.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.arny.allfy.R
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response


@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    userViewModel: UserViewModel
) {
    val currentUser by userViewModel.currentUser.collectAsState()

    when (currentUser) {
        is Response.Loading -> {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is Response.Success -> {
            EditProfile(onBackClick, (currentUser as Response.Success<User>).data, userViewModel)
        }

        is Response.Error -> {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (currentUser as Response.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
//                    .align(Alignment.CenterHorizontally)
                    .padding(start = 16.dp, end = 16.dp)
            )
        }
    }

}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditProfile(onBackClick: () -> Unit, initialUser: User, userViewModel: UserViewModel) {
    var user by remember { mutableStateOf(initialUser) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        userViewModel.updateUserProfile(user, selectedImageUri)
//                        onBackClick()
                    }) {
                        Text("Save")
                    }
                },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        user.imageUrl,
                        placeholder = rememberAsyncImagePainter(R.drawable.ic_user)
                    ),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier.size(100.dp),
                        contentScale = ContentScale.Crop
                    )
                }

            }

            Spacer(modifier = Modifier.height(16.dp))

            EditProfileField("Username", user.userName) { user = user.copy(userName = it) }
            EditProfileField("Bio", user.bio) { user = user.copy(bio = it) }
            EditProfileField("Email", user.email) { user = user.copy(email = it) }


            when (val response = userViewModel.updateProfileStatus.value) {
                is Response.Loading -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
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
                            .padding(start = 16.dp, end = 16.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun EditProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    TextField(
        label = { Text(label) },
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
        ),
    )
}