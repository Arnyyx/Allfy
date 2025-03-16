package com.arny.allfy.presentation.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.Toast
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    userViewModel: UserViewModel
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val updateStatus by userViewModel.updateProfileStatus.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var user by remember(currentUser) {
        mutableStateOf(
            if (currentUser is Response.Success) (currentUser as Response.Success<User>).data
            else User()
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    LaunchedEffect(updateStatus) {
        when (updateStatus) {
            is Response.Success -> {
                if ((updateStatus as Response.Success<Boolean>).data) {
                    toastMessage = "Profile updated successfully"
                    onBackClick()
                }
            }

            is Response.Error -> toastMessage = "Error: ${(updateStatus as Response.Error).message}"
            else -> Unit
        }
    }

    toastMessage?.let { message ->
        Toast(message)
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2000)
            toastMessage = null
        }
    }

    val isLoading = currentUser is Response.Loading || updateStatus is Response.Loading

    Scaffold(
        topBar = {
            Column {
                EditProfileTopBar(
                    onBackClick = onBackClick,
                    onSaveClick = { userViewModel.updateUserProfile(user, selectedImageUri) },
                    isLoading = isLoading
                )
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
            when (currentUser) {
                is Response.Success -> {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            ProfileImageSection(
                                user,
                                selectedImageUri
                            ) { launcher.launch("image/*") }
                            Spacer(modifier = Modifier.height(16.dp))
                            ProfileFieldsSection(user) { user = it }
                        }
                    }
                }

                is Response.Error -> ErrorContent((currentUser as Response.Error).message)
                is Response.Loading -> EditProfileSkeleton()
            }
        }
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
        title = {
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            TextButton(
                onClick = onSaveClick,
                enabled = !isLoading
            ) {
                Text(
                    text = "Save",
                    fontWeight = FontWeight.Bold,
                    color = if (!isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.5f
                    )
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
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
        AsyncImageWithPlaceholder(
            imageUrl = selectedImageUri?.toString() ?: currentUser.imageUrl,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            placeholderRes = R.drawable.ic_user
        )
    }
}

@Composable
private fun AsyncImageWithPlaceholder(
    imageUrl: String,
    modifier: Modifier = Modifier,
    placeholderRes: Int = R.drawable.ic_user
) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(if (imageUrl.isNotEmpty()) imageUrl else null)
            .crossfade(true)
            .placeholder(placeholderRes)
            .error(placeholderRes)
            .build()
    )
    Image(
        painter = painter,
        contentDescription = "Profile Picture",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun ProfileFieldsSection(
    currentUser: User,
    onUserUpdate: (User) -> Unit
) {
    EditProfileField(
        label = "Username",
        value = currentUser.username,
        onValueChange = {
            if (it.matches(Regex("^[a-zA-Z0-9_]*$"))) {
                onUserUpdate(currentUser.copy(username = it))
            }
        }
    )
    EditProfileField(
        label = "Name",
        value = currentUser.name,
        onValueChange = { onUserUpdate(currentUser.copy(name = it)) }
    )
    EditProfileField(
        label = "Email",
        value = currentUser.email,
        onValueChange = { onUserUpdate(currentUser.copy(email = it)) }
    )
    EditProfileField(
        label = "Bio",
        value = currentUser.bio,
        maxLength = 100,
        onValueChange = { if (it.length <= 100) onUserUpdate(currentUser.copy(bio = it)) }
    )
}

@Composable
private fun EditProfileField(
    label: String,
    value: String,
    maxLength: Int? = null,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        TextField(
            label = { Text(label) },
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = label != "Bio",
            maxLines = if (label == "Bio") 4 else 1
        )
        if (maxLength != null) {
            Text(
                text = "${value.length}/$maxLength",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun EditProfileSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
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
        repeat(4) {
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