package com.arny.allfy.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RecommendationApi {
    @GET("recommend/{userId}")
    suspend fun getRecommendations(
        @Path("userId") userId: String,
        @Query("limit") limit: Int
    ): Response<RecommendationResponse>
}

data class RecommendationResponse(
    val postIds: List<String>
)