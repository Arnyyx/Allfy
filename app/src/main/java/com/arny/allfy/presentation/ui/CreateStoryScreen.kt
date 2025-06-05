package com.arny.allfy.presentation.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.arny.allfy.domain.model.Story
import com.arny.allfy.presentation.viewmodel.StoryViewModel
import com.arny.allfy.utils.Response
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    navController: NavController,
    storyViewModel: StoryViewModel,
    userId: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val storyState by storyViewModel.storyState.collectAsState()
    var isUploading by remember { mutableStateOf(false) }

    val storyPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedUri = it
            isUploading = true
            val story = Story(userID = userId)
            storyViewModel.uploadStory(story, it)
        }
    }

    LaunchedEffect(Unit) {
        storyPickerLauncher.launch("image/*")
    }

    LaunchedEffect(storyState.uploadStoryState) {
        when (val uploadState = storyState.uploadStoryState) {
            is Response.Success -> {
                Toast.makeText(context, "Story uploaded successfully", Toast.LENGTH_SHORT).show()
                storyViewModel.resetUploadStoryState()
                navController.popBackStack()
            }

            is Response.Error -> {
                isUploading = false
                Toast.makeText(
                    context,
                    "Failed to upload story: ${uploadState.message}",
                    Toast.LENGTH_LONG
                ).show()
                storyViewModel.resetUploadStoryState()
            }

            else -> {}
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Create Story") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                if (isUploading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (!isUploading) {
                Text("Select an image to create a story")
            }
        }
    }
}