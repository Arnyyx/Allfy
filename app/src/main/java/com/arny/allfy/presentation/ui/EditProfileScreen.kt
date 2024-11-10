package com.arny.allfy.presentation.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.arny.allfy.R
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.Toast
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response

@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    userViewModel: UserViewModel
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val updateStatus by userViewModel.updateProfileStatus.collectAsState()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    if (updateStatus is Response.Error) {
        Text((updateStatus as Response.Error).message)
    }

    Scaffold(
        topBar = {
            EditProfileTopBar(
                onBackClick = onBackClick,
                onSaveClick = {
                    if (currentUser is Response.Success) {
                        userViewModel.updateUserProfile(
                            (currentUser as Response.Success<User>).data,
                            selectedImageUri
                        )
                    }
                },
                isLoading = updateStatus is Response.Loading
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Loading indicator under TopAppBar
            if (currentUser is Response.Loading || updateStatus is Response.Loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Main content
            when (currentUser) {
                is Response.Success -> {
                    var user by remember { mutableStateOf((currentUser as Response.Success<User>).data) }


                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        ProfileImageSection(
                            currentUser = user,
                            selectedImageUri = selectedImageUri,
                            onImageClick = { launcher.launch("image/*") }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileFieldsSection(
                            currentUser = user,
                            onUserUpdate = { updatedUser ->
                                user = updatedUser
                            }
                        )
                    }

                }

                is Response.Error -> {
                    ErrorContent(errorMessage = (currentUser as Response.Error).message)
                }

                is Response.Loading -> {
                    // Show skeleton loading state
                    EditProfileSkeleton()
                }
            }
        }
    }
}

@Composable
private fun EditProfileSkeleton() {
    // Show loading state while maintaining the UI structure
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Profile image placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Field placeholders
        repeat(3) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}
        }
    }
}

@Composable
private fun ErrorContent(errorMessage: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileTopBar(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    isLoading: Boolean
) {
    TopAppBar(
        title = { Text("Edit Profile") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            TextButton(
                onClick = onSaveClick,
                enabled = !isLoading
            ) {
                Text("Save")
            }
        }
    )
}

@Composable
private fun EditProfileContent(
    user: User,
    userViewModel: UserViewModel,
    updateStatus: Response<Boolean>
) {

}

@Composable
private fun ProfileImageSection(
    currentUser: User,
    selectedImageUri: Uri?,
    onImageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clickable(onClick = onImageClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = selectedImageUri ?: currentUser.imageUrl,
                placeholder = rememberAsyncImagePainter(R.drawable.ic_user)
            ),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ProfileFieldsSection(
    currentUser: User,
    onUserUpdate: (User) -> Unit
) {
    EditProfileField("Username", currentUser.userName) {
        onUserUpdate(currentUser.copy(userName = it))
    }
    EditProfileField("Bio", currentUser.bio) {
        onUserUpdate(currentUser.copy(bio = it))
    }
    EditProfileField("Email", currentUser.email) {
        onUserUpdate(currentUser.copy(email = it))
    }
}

@Composable
private fun EditProfileField(
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
        )
    )
}