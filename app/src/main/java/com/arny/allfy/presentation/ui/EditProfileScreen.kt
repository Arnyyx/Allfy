package com.arny.allfy.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arny.allfy.R
import com.arny.allfy.domain.model.User
import com.arny.allfy.ui.theme.Transparent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onBackClick: () -> Unit) {

    val user = User("" ,"", )

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
                    TextButton(onClick = { /* Save changes */ }) {
                        Text("Done", color = Color.Blue)
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
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_user),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            TextButton(
                onClick = { /* Change profile picture */ },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Change Profile Photo", color = Color.Blue)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Edit Fields
            EditProfileField("Name", user.name) { user.name = it }
            EditProfileField("Username", user.userName) { user.userName = it }
            EditProfileField("Bio", user.bio) { user.bio = it }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

//            Text(
//                "Private Information",
//                fontWeight = FontWeight.Bold,
//                fontSize = 16.sp,
//                modifier = Modifier.padding(vertical = 8.dp)
//            )
//
//            EditProfileField("Email", email) { email = it }
//            EditProfileField("Phone", phone) { phone = it }
//            EditProfileField("Gender", gender) { gender = it }
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
            focusedContainerColor = Transparent,
            unfocusedContainerColor = Transparent,
            disabledContainerColor = Transparent,
            errorContainerColor = Transparent,
        ),
    )
}

@Preview
@Composable
fun EditProfileScreenPreview() {
    EditProfileScreen(onBackClick = {})
}