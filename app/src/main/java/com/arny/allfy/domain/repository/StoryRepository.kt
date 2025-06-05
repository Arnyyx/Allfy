package com.arny.allfy.domain.repository

import android.net.Uri
import com.arny.allfy.domain.model.Story
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    suspend fun uploadStory(story: Story, mediaUri: Uri): Flow<Response<Boolean>>
    suspend fun getUserStories(userID: String): Flow<Response<List<Story>>>
    suspend fun getStoriesByUserIds(userIds: List<String>): Flow<Response<List<Story>>>
    suspend fun logStoryView(userID: String, storyID: String): Flow<Response<Boolean>>
    suspend fun deleteStory(storyID: String, userID: String): Flow<Response<Boolean>>
}