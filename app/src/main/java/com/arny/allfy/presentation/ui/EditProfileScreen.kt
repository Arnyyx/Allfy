package com.arny.allfy.presentation.ui

import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.state.UserState
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.getDataOrNull
import com.arny.allfy.utils.getErrorMessageOrNull
import com.arny.allfy.utils.isError
import com.arny.allfy.utils.isLoading
import com.arny.allfy.utils.isSuccess

@Composable
fun EditProfileScreen(
    navController: NavHostController,
    userViewModel: UserViewModel
) {
    val userState by userViewModel.userState.collectAsState()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val currentUser = userState.currentUserState.getDataOrNull() ?: User()
    var editedUser by remember(currentUser) {
        mutableStateOf(currentUser)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    LaunchedEffect(userState.updateProfileState) {
        when {
            userState.updateProfileState.isLoading -> {}

            userState.updateProfileState.isSuccess -> {
                userViewModel.getCurrentUser(currentUser.userId)
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                userViewModel.resetUpdateProfileState()
                navController.popBackStack()
            }

            userState.updateProfileState.isError -> {
                userState.updateProfileState.getErrorMessageOrNull()?.let { error ->
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                EditProfileTopBar(
                    userState = userState,
                    navController = navController,
                    onSaveClick = {
                        userViewModel.updateUserProfile(editedUser, selectedImageUri)
                    },
                    hasChanges = editedUser != currentUser || selectedImageUri != null
                )
                if (userState.updateProfileState.isLoading) {
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
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    ProfileImageSection(
                        user = editedUser,
                        selectedImageUri = selectedImageUri
                    ) {
                        launcher.launch("image/*")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileFieldsSection(
                        user = editedUser,
                        onUserUpdate = { updatedUser ->
                            editedUser = updatedUser
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileTopBar(
    userState: UserState,
    navController: NavHostController,
    onSaveClick: () -> Unit,
    hasChanges: Boolean
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
            IconButton(onClick = {
                navController.popBackStack()
            }) {
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
                enabled = !userState.updateProfileState.isLoading && hasChanges
            ) {
                Text(
                    text = "Save",
                    fontWeight = FontWeight.Bold,
                    color = if (!userState.updateProfileState.isLoading && hasChanges)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
    user: User,
    selectedImageUri: Uri?,
    onImageClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clickable(onClick = onImageClick),
            contentAlignment = Alignment.Center
        ) {
            AsyncImageWithPlaceholder(
                imageUrl = selectedImageUri?.toString() ?: user.imageUrl,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                placeholderRes = R.drawable.ic_user
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap to change photo",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
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
    user: User,
    onUserUpdate: (User) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EditProfileField(
            label = "Email",
            value = user.email,
            onValueChange = { newValue ->
                onUserUpdate(user.copy(email = newValue))
            },
            enabled = false,
        )

        EditProfileField(
            label = "Username",
            value = user.username,
            onValueChange = { newValue ->
                if (newValue.matches(Regex("^[a-zA-Z0-9_]*$"))) {
                    onUserUpdate(user.copy(username = newValue))
                }
            },
            helper = "Only letters, numbers, and underscores allowed",
        )

        EditProfileField(
            label = "Name",
            value = user.name,
            onValueChange = { newValue ->
                onUserUpdate(user.copy(name = newValue))
            },
        )
        EditProfileField(
            label = "Bio",
            value = user.bio ?: "",
            maxLength = 150,
            onValueChange = { newValue ->
                if (newValue.length <= 150) {
                    onUserUpdate(user.copy(bio = newValue))
                }
            },
            singleLine = false,
            maxLines = 4,
        )
    }
}

@Composable
private fun EditProfileField(
    label: String,
    value: String,
    maxLength: Int? = null,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    helper: String? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            label = { Text(label) },
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(12.dp),
            supportingText = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = helper ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (maxLength != null) {
                        Text(
                            text = "${value.length}/$maxLength",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (value.length == maxLength)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )
    }
}