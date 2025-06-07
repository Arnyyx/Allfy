package com.arny.allfy.domain.usecase.story

import android.net.Uri
import com.arny.allfy.domain.model.Story
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.repository.StoryRepository
import com.arny.allfy.utils.Response
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StoryUseCases @Inject constructor(
    private val storyRepository: StoryRepository
) {
    suspend fun uploadStory(story: Story, mediaUri: Uri): Flow<Response<Boolean>> {
        return storyRepository.uploadStory(story, mediaUri)
    }

    suspend fun getUserStories(userId: String): Flow<Response<List<Story>>> {
        return storyRepository.getUserStories(userId)
    }

    suspend fun getStoriesByUserIds(userIds: List<String>): Flow<Response<List<Story>>> {
        return storyRepository.getStoriesByUserIds(userIds)
    }

    suspend fun logStoryView(userId: String, storyId: String): Flow<Response<Boolean>> {
        return storyRepository.logStoryView(userId, storyId)
    }

    suspend fun deleteStory(storyId: String, userId: String): Flow<Response<Boolean>> {
        return storyRepository.deleteStory(storyId, userId)
    }
}
