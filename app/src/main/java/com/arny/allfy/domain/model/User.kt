package com.arny.allfy.domain.model

import android.net.Uri

data class User(
    var name: String = "",
    var userName: String = "",
    var userID: String = "",
    var email: String = "",
    var password: String = "",
    var imageUrl: String = "",
    var bio: String = "",
    var following: List<String> = emptyList(),
    var followers: List<String> = emptyList(),
    var totalPosts: String = "0",
)