package com.arny.allfy.domain.model

data class User(
    val userId: String = "",
    val username: String = "",
    val name: String = "",
    val email: String = "",
    val imageUrl: String = "",
    val bio: String? = "",
    val fcmToken: String = "",
    val hasStory: Boolean = false
)