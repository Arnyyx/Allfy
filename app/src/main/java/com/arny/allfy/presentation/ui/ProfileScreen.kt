package com.arny.allfy.presentation.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arny.allfy.R
import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.common.BottomNavigation
import com.arny.allfy.presentation.common.BottomNavigationItem
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screen
import com.arny.allfy.utils.getDataOrNull
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.arny.allfy.utils.handleQRResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel,
    userId: String? = null
) {
    val userState by userViewModel.userState.collectAsState()
    val postState by postViewModel.postState.collectAsState()
    val context = LocalContext.current
    val isCurrentUser = userId == null
    var showQrOptionsDialog by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            navController.navigate(Screen.QRScannerScreen)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            qrCodeBitmap?.let { bitmap ->
                saveQRCodeToGallery(context, bitmap) { success ->
                    Toast.makeText(
                        context,
                        if (success) "QR Code saved to gallery" else "Failed to save QR Code",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            isProcessingImage = true
            coroutineScope.launch {
                try {
                    val qrResult = decodeQRFromImage(context, selectedUri)
                    withContext(Dispatchers.Main) {
                        isProcessingImage = false
                        if (qrResult != null) {
                            handleQRResult(qrResult, navController, context)
                        } else {
                            Toast.makeText(context, "No QR code found in image", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isProcessingImage = false
                        Toast.makeText(context, "Error reading image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(userId) {
        if (userId != null) {
            userViewModel.getUserDetails(userId)
        }
    }

    LaunchedEffect(userState.currentUserState, userState.otherUserState) {
        val userResponse =
            if (isCurrentUser) userState.currentUserState else userState.otherUserState
        if (userResponse is Response.Success) {
            val user = userResponse.data
            val qrContent = "allfy://profile/${user.userId}"
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512)
            qrCodeBitmap = BarcodeEncoder().createBitmap(bitMatrix)
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
                                IconButton(onClick = { navController.navigate(Screen.CreatePostScreen) }) {
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
                onAvatarClick = { imageUrl ->
                    showImageViewer = true
                }
            )
        }

        if (showQrOptionsDialog) {
            QROptionsDialog(
                onDismiss = { showQrOptionsDialog = false },
                onShowQRCode = {
                    showQrOptionsDialog = false
                    showQrCodeDialog = true
                },
                onScanWithCamera = {
                    showQrOptionsDialog = false
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        navController.navigate(Screen.QRScannerScreen)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onScanFromGallery = {
                    showQrOptionsDialog = false
                    imagePickerLauncher.launch("image/*")
                }
            )
        }

        if (showQrCodeDialog && qrCodeBitmap != null) {
            QRCodeDisplayDialog(
                qrCodeBitmap = qrCodeBitmap!!,
                username = (userState.currentUserState as? Response.Success)?.data?.username ?: "",
                onDismiss = { showQrCodeDialog = false },
                onSaveToGallery = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        saveQRCodeToGallery(context, qrCodeBitmap!!) { success ->
                            Toast.makeText(
                                context,
                                if (success) "QR Code saved to gallery" else "Failed to save QR Code",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            saveQRCodeToGallery(context, qrCodeBitmap!!) { success ->
                                Toast.makeText(
                                    context,
                                    if (success) "QR Code saved to gallery" else "Failed to save QR Code",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                }
            )
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

@Composable
private fun QROptionsDialog(
    onDismiss: () -> Unit,
    onShowQRCode: () -> Unit,
    onScanWithCamera: () -> Unit,
    onScanFromGallery: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QR Code Options",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                QROptionItem(
                    icon = Icons.Default.QrCode,
                    title = "Share My Profile",
                    subtitle = "Show QR code for others to scan",
                    onClick = onShowQRCode
                )
                Spacer(modifier = Modifier.height(12.dp))
                QROptionItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Scan with Camera",
                    subtitle = "Open camera to scan QR code",
                    onClick = onScanWithCamera
                )
                Spacer(modifier = Modifier.height(12.dp))
                QROptionItem(
                    icon = Icons.Default.Image,
                    title = "Scan from Gallery",
                    subtitle = "Choose QR code image from gallery",
                    onClick = onScanFromGallery
                )
            }
        }
    }
}

@Composable
private fun QROptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun QRCodeDisplayDialog(
    qrCodeBitmap: Bitmap,
    username: String,
    onDismiss: () -> Unit,
    onSaveToGallery: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share @$username",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .size(240.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                ) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = "Profile QR Code",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Scan this QR code to view @$username's profile",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSaveToGallery,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Save",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save to Gallery")
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

private suspend fun decodeQRFromImage(context: Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            MultiFormatReader().decode(binaryBitmap).text
        } catch (e: Exception) {
            Log.e("QRDecode", "Error decoding QR from image", e)
            null
        }
    }
}

private fun saveQRCodeToGallery(context: Context, bitmap: Bitmap, onComplete: (Boolean) -> Unit) {
    try {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "QR_Code_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Allfy")
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                onComplete(true)
            } ?: onComplete(false)
        } ?: onComplete(false)
    } catch (e: Exception) {
        Log.e("SaveQR", "Error saving QR code", e)
        onComplete(false)
    }
}

@Composable
private fun ProfileContent(
    navController: NavController,
    userState: com.arny.allfy.presentation.state.UserState,
    postViewModel: PostViewModel,
    userViewModel: UserViewModel,
    isCurrentUser: Boolean,
    paddingValues: PaddingValues,
    onAvatarClick: (String) -> Unit
) {
    val userResponse = if (isCurrentUser) userState.currentUserState else userState.otherUserState
    when (userResponse) {
        is Response.Loading -> {}
        is Response.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userResponse.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        is Response.Success -> {
            ProfileDetails(
                navController = navController,
                user = userResponse.data,
                followingCount = (userState.followingCountState as? Response.Success)?.data ?: 0,
                followersCount = (userState.followersCountState as? Response.Success)?.data ?: 0,
                postsIds = (userState.postsIdsState as? Response.Success)?.data ?: emptyList(),
                isFollowing = (userState.checkIfFollowingState as? Response.Success)?.data ?: false,
                isCurrentUser = isCurrentUser,
                userViewModel = userViewModel,
                postViewModel = postViewModel,
                paddingValues = paddingValues,
                currentUser = if (!isCurrentUser) userState.currentUserState.getDataOrNull() else null,
                onAvatarClick = onAvatarClick
            )
        }

        is Response.Idle -> {}
    }
}

@Composable
private fun ProfileDetails(
    navController: NavController,
    user: User,
    followingCount: Int,
    followersCount: Int,
    postsIds: List<String>,
    isFollowing: Boolean,
    isCurrentUser: Boolean,
    userViewModel: UserViewModel,
    postViewModel: PostViewModel,
    paddingValues: PaddingValues,
    currentUser: User?,
    onAvatarClick: (String) -> Unit
) {
    var isFollowingState by remember(key1 = isFollowing) { mutableStateOf(isFollowing) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImageWithPlaceholder(
                imageUrl = user.imageUrl,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), CircleShape)
                    .clickable { onAvatarClick(user.imageUrl) }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem("Posts", postsIds.size.toString())
                StatisticItem("Followers", followersCount.toString())
                StatisticItem("Following", followingCount.toString())
            }
        }
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(user.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (!user.bio.isNullOrEmpty()) {
                Text(
                    user.bio,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 18.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isCurrentUser) {
            OutlinedButton(
                onClick = { navController.navigate(Screen.EditProfileScreen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text("Edit Profile", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        } else if (currentUser != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isFollowingState) {
                    OutlinedButton(
                        onClick = {
                            userViewModel.unfollowUser(currentUser.userId, user.userId)
                            isFollowingState = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Following", fontSize = 14.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            userViewModel.followUser(currentUser.userId, user.userId)
                            isFollowingState = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Follow", fontSize = 14.sp)
                    }
                }
                OutlinedButton(
                    onClick = { navController.navigate(Screen.ChatScreen(otherUserId = user.userId)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Message", fontSize = 14.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                icon = {
                    Icon(
                        painterResource(id = R.drawable.placehoder_image),
                        contentDescription = null
                    )
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                icon = {
                    Icon(
                        painterResource(id = R.drawable.ic_comment),
                        contentDescription = null
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        PostsGrid(
            navController = navController,
            postsIds = postsIds,
            postViewModel = postViewModel,
            showMediaPosts = selectedTabIndex == 0
        )
    }
}

@Composable
private fun StatisticItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun PostsGrid(
    navController: NavController,
    postsIds: List<String>,
    postViewModel: PostViewModel,
    showMediaPosts: Boolean
) {
    LaunchedEffect(postsIds) {
        if (postsIds.isNotEmpty()) {
            postViewModel.getPostsByIds(postsIds)
        }
    }
    val postState by postViewModel.postState.collectAsState()
    when (val postsResponse = postState.getPostsState) {
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
                Text(text = postsResponse.message, color = MaterialTheme.colorScheme.error)
            }
        }

        is Response.Success -> {
            val loadedPosts = postsResponse.data.associateBy { it.postID }
            val filteredPostIds = postsIds.filter { postId ->
                loadedPosts[postId]?.let { post ->
                    if (showMediaPosts) post.mediaItems.isNotEmpty() else post.mediaItems.isEmpty()
                } ?: false
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (showMediaPosts) 3 else 1),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(filteredPostIds, key = { _, postId -> postId }) { _, postId ->
                    if (showMediaPosts) {
                        AnimatedPostThumbnail(postId, loadedPosts, navController)
                    } else {
                        TextOnlyPostItem(postId, loadedPosts, navController)
                    }
                }
            }
        }

        is Response.Idle -> {}
    }
}

@Composable
private fun TextOnlyPostItem(
    postId: String,
    loadedPosts: Map<String, Post>,
    navController: NavController
) {
    val post = loadedPosts[postId] ?: return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .clickable { navController.navigate(Screen.PostDetailScreen(postId)) }
            .padding(12.dp)
    ) {
        Text(
            text = post.caption,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AnimatedPostThumbnail(
    postId: String,
    loadedPosts: Map<String, Post>,
    navController: NavController
) {
    val post = loadedPosts[postId] ?: return
    val context = LocalContext.current
    val firstMediaItem = post.mediaItems.firstOrNull()
    val imageUrl = when (firstMediaItem?.mediaType) {
        "image" -> firstMediaItem.url
        "video" -> firstMediaItem.thumbnailUrl
        else -> null
    }
    AnimatedVisibility(
        visible = imageUrl != null,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = fadeOut()
    ) {
        imageUrl?.let {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(it)
                    .placeholder(R.drawable.placehoder_image)
                    .build(),
                contentDescription = "Post Thumbnail",
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { navController.navigate(Screen.PostDetailScreen(postId)) },
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun AsyncImageWithPlaceholder(imageUrl: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(if (imageUrl.isNotEmpty()) imageUrl else null)
            .crossfade(true)
            .placeholder(R.drawable.ic_user)
            .error(R.drawable.ic_user)
            .build(),
        contentDescription = "Profile Picture",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}