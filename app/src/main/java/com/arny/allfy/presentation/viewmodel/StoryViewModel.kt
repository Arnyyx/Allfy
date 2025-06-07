package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.Story
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.usecase.story.StoryUseCases
import com.arny.allfy.presentation.state.StoryState
import com.arny.allfy.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val storyUseCases: StoryUseCases
) : ViewModel() {

    private val _storyState = MutableStateFlow(StoryState())
    val storyState: StateFlow<StoryState> = _storyState.asStateFlow()

    fun uploadStory(story: Story, mediaUri: Uri) {
        viewModelScope.launch {
            storyUseCases.uploadStory(story, mediaUri).collect { response ->
                _storyState.update { it.copy(uploadStoryState = response) }
            }
        }
    }

    fun getUserStories(userId: String) {
        viewModelScope.launch {
            storyUseCases.getUserStories(userId).collect { response ->
                _storyState.update { it.copy(userStoriesState = response) }
            }
        }
    }

    fun getStoriesByUserIds(userIds: List<String>) {
        viewModelScope.launch {
            storyUseCases.getStoriesByUserIds(userIds).collect { response ->
                _storyState.update { it.copy(storiesByUsersState = response) }
            }
        }
    }

    fun logStoryView(userId: String, storyId: String) {
        viewModelScope.launch {
            storyUseCases.logStoryView(userId, storyId).collect { response ->
                _storyState.update { it.copy(logStoryViewState = response) }
            }
        }
    }

    fun deleteStory(storyId: String, userId: String) {
        viewModelScope.launch {
            storyUseCases.deleteStory(storyId, userId).collect { response ->
                _storyState.update { it.copy(deleteStoryState = response) }
            }
        }
    }

    fun resetUploadStoryState() {
        _storyState.update { it.copy(uploadStoryState = Response.Idle) }
    }

    fun resetUserStoriesState() {
        _storyState.update { it.copy(userStoriesState = Response.Idle) }
    }

    fun resetStoriesByUsersState() {
        _storyState.update { it.copy(storiesByUsersState = Response.Idle) }
    }

    fun resetLogStoryViewState() {
        _storyState.update { it.copy(logStoryViewState = Response.Idle) }
    }

    fun resetDeleteStoryState() {
        _storyState.update { it.copy(deleteStoryState = Response.Idle) }
    }

    fun clearStoryState() {
        _storyState.value = StoryState()
    }
}
