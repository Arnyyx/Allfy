package com.arny.allfy.presentation.state

import com.arny.allfy.domain.model.Story
import com.arny.allfy.utils.Response

data class StoryState(
    val uploadStoryState: Response<Boolean> = Response.Idle,
    val userStoriesState: Response<List<Story>> = Response.Idle,
    val storiesByUsersState: Response<List<Story>> = Response.Idle,
    val logStoryViewState: Response<Boolean> = Response.Idle,
    val deleteStoryState: Response<Boolean> = Response.Idle
)
