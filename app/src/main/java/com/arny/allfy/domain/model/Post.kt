package com.arny.allfy.domain.model

data class Post (
    val postID: String = "",
    val postImage: String = "",
    val postDescription: String = "",
    val postedBy: User = User(),
    val likedBy: List<User> = emptyList(),
//    val comments: List<Comment> = emptyList(),
    val timeStamp: Long? = null
)