package com.arny.allfy.presentation.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.arny.allfy.R
import com.arny.allfy.domain.model.User
import com.arny.allfy.presentation.viewmodel.UserViewModel
import com.arny.allfy.presentation.viewmodel.ChatViewModel
import com.arny.allfy.utils.Response

@Composable
fun IncomingCallScreen(
    callerId: String,
    calleeId: String,
    callId: String,
    userViewModel: UserViewModel,
    chatViewModel: ChatViewModel,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Log.d("IncomingCallScreen", "CallerId: $callerId, CalleeId: $calleeId, CallId: $callId")
    Column {
        Text("Incoming Call from $callerId")
        Button(onClick = onAccept) {
            Text("Accept")
        }
        Button(onClick = onReject) {
            Text("Reject")
        }
    }
}