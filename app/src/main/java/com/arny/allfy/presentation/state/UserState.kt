package com.arny.allfy.presentation.state

import com.arny.allfy.domain.model.User
import com.arny.allfy.utils.Response

data class UserState(
    val currentUserState: Response<User> = Response.Idle,
    val otherUserState: Response<User> = Response.Idle,
    val updateProfileState: Response<Boolean> = Response.Idle,
    val followUserState: Response<Boolean> = Response.Idle,
    val unfollowUserState: Response<Boolean> = Response.Idle,
    val followersState: Response<List<User>> = Response.Idle,
    val followingCountState: Response<Int> = Response.Idle,
    val followersCountState: Response<Int> = Response.Idle,
    val postsIdsState: Response<List<String>> = Response.Idle,
    val checkIfFollowingState: Response<Boolean> = Response.Idle,
    val usersState: Response<List<User>> = Response.Idle
)
