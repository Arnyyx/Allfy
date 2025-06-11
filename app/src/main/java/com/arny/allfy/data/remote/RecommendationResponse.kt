package com.arny.allfy.data.remote

import com.arny.allfy.domain.model.Post
import com.arny.allfy.domain.model.User

data class RecommendationResponse(
    val posts: List<RecommendedPost>
)

data class RecommendedPost(
    val postId: String,
    val score: Float,
    val reason: String
)