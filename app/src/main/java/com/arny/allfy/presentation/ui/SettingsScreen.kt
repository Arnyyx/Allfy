package com.arny.allfy.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arny.allfy.R
import com.arny.allfy.presentation.common.Dialog
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.presentation.viewmodel.PostViewModel
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel,
    postViewModel: PostViewModel,
    userViewModel: UserViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(authState.signOutState, authState.isAuthenticated) {
        when (authState.signOutState) {
            is Response.Success -> {
                if (!authState.isAuthenticated) {
                    chatViewModel.clearChatState()
                    postViewModel.clearPostState()
                    userViewModel.clearUserState()
                    navController.navigate(Screen.SplashScreen) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                    authViewModel.clearAuthState()
                }
            }

            is Response.Error -> {}

            else -> {}
        }
    }

    val isLoading = authState.signOutState is Response.Loading

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.Close, contentDescription = "Back")
                        }
                    }
                )
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item { SettingsSection("Account") }
                items(accountSettings) { setting ->
                    SettingsItem(setting)
                }

                item { SettingsSection("Privacy") }
                items(privacySettings) { setting ->
                    SettingsItem(setting)
                }

                item { SettingsSection("Help") }
                items(helpSettings) { setting ->
                    SettingsItem(setting)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Log Out")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (showLogoutDialog) {

                Dialog(
                    title = "Confirm Logout",
                    message = "Are you sure you want to log out?",
                    confirmText = "Yes",
                    dismissText = "Cancel",
                    onConfirm = {
                        authViewModel.signOut()
                        showLogoutDialog = false
                    },
                    onDismiss = { showLogoutDialog = false }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(setting: SettingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle click */ }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = setting.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = setting.title,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class SettingItem(val title: String, val icon: Int)

val accountSettings = listOf(
    SettingItem("Personal Information", R.drawable.ic_person),
    SettingItem("Saved", R.drawable.placehoder_image),
    SettingItem("Close Friends", R.drawable.ic_star),
    SettingItem("Language", R.drawable.ic_language)
)

val privacySettings = listOf(
    SettingItem("Privacy and Security", R.drawable.ic_lock),
    SettingItem("Ads", R.drawable.ic_advertisment),
    SettingItem("Login Activity", R.drawable.ic_activity)
)

val helpSettings = listOf(
    SettingItem("Report a Problem", R.drawable.ic_report),
    SettingItem("Help Center", R.drawable.ic_help),
    SettingItem("About", R.drawable.ic_info)
)