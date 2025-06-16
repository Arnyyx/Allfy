package com.arny.allfy.presentation.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.BottomNavigation
import com.arny.allfy.presentation.common.BottomNavigationItem
import com.arny.allfy.presentation.components.ProfileBottomSheet
import com.arny.allfy.presentation.components.ProfileContent
import com.arny.allfy.presentation.components.QRCodeDisplayDialog
import com.arny.allfy.presentation.components.QROptionsDialog
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.StoryViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.CameraPermissionLauncher
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screen
import com.arny.allfy.utils.StoragePermissionLauncher
import com.arny.allfy.utils.generateQRCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel,
    storyViewModel: StoryViewModel,
    userId: String? = null
) {
    val userState by userViewModel.userState.collectAsState()
    val postState by postViewModel.postState.collectAsState()
    val storyState by storyViewModel.storyState.collectAsState()
    val context = LocalContext.current
    var showQrOptionsDialog by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val isProcessingImage by remember { mutableStateOf(false) }

    val currentUserId = (userState.currentUserState as? Response.Success)?.data?.userId
    val isCurrentUser = userId == null || userId == currentUserId

    val cameraPermissionLauncher = CameraPermissionLauncher(navController) {
        Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }
    val storagePermissionLauncher = StoragePermissionLauncher(context, qrCodeBitmap) {
        Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(userId) {
        if (userId != null) {
            userViewModel.getUserDetails(userId)
        }
    }

    LaunchedEffect(storyState.uploadStoryState) {
        when (storyState.uploadStoryState) {
            is Response.Success -> {
                if (isCurrentUser && currentUserId != null) {
                    userViewModel.getCurrentUser(currentUserId)
                } else if (!isCurrentUser && userId != null) {
                    userViewModel.getUserDetails(userId)
                }
                storyViewModel.resetUploadStoryState()
            }

            else -> {}
        }
    }

    LaunchedEffect(userState.currentUserState, userState.otherUserState) {
        val userResponse =
            if (isCurrentUser) userState.currentUserState else userState.otherUserState
        if (userResponse is Response.Success) {
            val user = userResponse.data
            val qrContent = "allfy://profile/${user.userId}"
            qrCodeBitmap = generateQRCode(qrContent)
            userViewModel.getFollowingCount(user.userId)
            userViewModel.getFollowersCount(user.userId)
            userViewModel.getPostIds(user.userId)
        }
    }

    LaunchedEffect(userState.currentUserState, userState.otherUserState) {
        if (!isCurrentUser && userState.currentUserState is Response.Success && userState.otherUserState is Response.Success) {
            val currentUser = (userState.currentUserState as Response.Success<User>).data
            val otherUser = (userState.otherUserState as Response.Success<User>).data
            userViewModel.checkIfFollowing(currentUser.userId, otherUser.userId)
        }
    }

    LaunchedEffect(postState.deletePostState) {
        when (val deleteState = postState.deletePostState) {
            is Response.Success -> {
                Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                val userResponse =
                    if (isCurrentUser) userState.currentUserState else userState.otherUserState
                if (userResponse is Response.Success) {
                    userViewModel.getPostIds(userResponse.data.userId)
                }
                postViewModel.resetDeletePostState()
            }

            is Response.Error -> {
                Toast.makeText(
                    context,
                    "Failed to delete post: ${deleteState.message}",
                    Toast.LENGTH_LONG
                ).show()
                postViewModel.resetDeletePostState()
            }

            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            val username = when (val userResponse =
                                if (isCurrentUser) userState.currentUserState else userState.otherUserState) {
                                is Response.Success -> userResponse.data.username
                                else -> ""
                            }
                            Text(
                                text = username,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            if (!isCurrentUser) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                }
                            }
                        },
                        actions = {
                            if (isCurrentUser) {
                                IconButton(onClick = { navController.navigate(Screen.PostEditorScreen()) }) {
                                    Icon(Icons.Default.Add, "New Post")
                                }
                                IconButton(onClick = { showQrOptionsDialog = true }) {
                                    Icon(Icons.Default.QrCode, "QR Options")
                                }
                                IconButton(onClick = { navController.navigate(Screen.SettingsScreen) }) {
                                    Icon(Icons.Default.Menu, "Settings")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )

                    val isLoading =
                        if (isCurrentUser) userState.currentUserState is Response.Loading
                        else userState.otherUserState is Response.Loading || userState.currentUserState is Response.Loading

                    AnimatedVisibility(visible = isLoading || postState.deletePostState is Response.Loading || isProcessingImage) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = when {
                                postState.deletePostState is Response.Loading -> MaterialTheme.colorScheme.error
                                isProcessingImage -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            },
            bottomBar = {
                if (isCurrentUser) BottomNavigation(BottomNavigationItem.Profile, navController)
            }
        ) { paddingValues ->
            ProfileContent(
                navController = navController,
                userState = userState,
                postViewModel = postViewModel,
                userViewModel = userViewModel,
                isCurrentUser = isCurrentUser,
                paddingValues = paddingValues,
                onAvatarClick = {
                    val userResponse =
                        if (isCurrentUser) userState.currentUserState else userState.otherUserState
                    val user = (userResponse as? Response.Success)?.data
                    user?.let {
                        if (it.hasStory) {
                            showBottomSheet = true
                        } else {
                            showImageViewer = true
                        }
                    }
                },
                onCreateStoryClick = { navController.navigate(Screen.CreateStoryScreen) }
            )
        }

        if (showQrOptionsDialog) {
            QROptionsDialog(
                onDismiss = { showQrOptionsDialog = false },
                onShowQRCode = {
                    showQrOptionsDialog = false
                    showQrCodeDialog = true
                },
                onScanWithCamera = cameraPermissionLauncher
            )
        }

        if (showQrCodeDialog && qrCodeBitmap != null) {
            QRCodeDisplayDialog(
                qrCodeBitmap = qrCodeBitmap!!,
                username = (userState.currentUserState as? Response.Success)?.data?.username ?: "",
                onDismiss = { showQrCodeDialog = false },
                onSaveToGallery = storagePermissionLauncher
            )
        }

        if (showBottomSheet) {
            val userResponse =
                if (isCurrentUser) userState.currentUserState else userState.otherUserState
            val user = (userResponse as? Response.Success)?.data
            user?.let {
                ProfileBottomSheet(
                    hasStories = it.hasStory,
                    onDismiss = { showBottomSheet = false },
                    onViewAvatar = { showImageViewer = true },
                    onViewStories = {
                        navController.navigate(
                            Screen.StoryViewerScreen(
                                it.userId,
                                isCurrentUser
                            )
                        )
                    }
                )
            }
        }

        if (showImageViewer) {
            val userResponse =
                if (isCurrentUser) userState.currentUserState else userState.otherUserState
            val imageUrl = (userResponse as? Response.Success)?.data?.imageUrl ?: ""
            if (imageUrl.isNotEmpty()) {
                ImageViewerScreen(
                    imageUrl = imageUrl,
                    onDismiss = { showImageViewer = false }
                )
            }
        }
    }
}