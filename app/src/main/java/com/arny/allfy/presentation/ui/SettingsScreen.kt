package com.arny.allfy.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arny.allfy.presentation.viewmodel.AuthViewModel
import com.arny.allfy.utils.Screens

//@Composable
//@Preview
//fun SettingPreview() {
//    val navController = NavController(LocalContext.current)
//    SettingsScreen(navController)
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController, authViewModel: AuthViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SettingsSection("Account")
            }
            items(accountSettings) { setting ->
                SettingsItem(setting)
            }

            item {
                SettingsSection("Privacy")
            }
            items(privacySettings) { setting ->
                SettingsItem(setting)
            }

            item {
                SettingsSection("Help")
            }
            items(helpSettings) { setting ->
                SettingsItem(setting)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        authViewModel.signOut()
                        navController.navigate(Screens.LoginScreen.route) {
                            popUpTo(Screens.FeedScreen.route) {
                                inclusive = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Log Out")
                }
                Spacer(modifier = Modifier.height(16.dp))
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
            imageVector = setting.icon,
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
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class SettingItem(val title: String, val icon: ImageVector)

val accountSettings = listOf(
    SettingItem("Personal Information", Icons.Default.Person),
    SettingItem("Saved", Icons.Default.Person),
    SettingItem("Close Friends", Icons.Default.Person),
    SettingItem("Language", Icons.Default.Person)
)

val privacySettings = listOf(
    SettingItem("Privacy and Security", Icons.Default.Lock),
    SettingItem("Ads", Icons.Default.AddCircle),
    SettingItem("Login Activity", Icons.Default.Person)
)

val helpSettings = listOf(
    SettingItem("Report a Problem", Icons.Default.Person),
    SettingItem("Help Center", Icons.Default.Person),
    SettingItem("About", Icons.Default.Info)
)