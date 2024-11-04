package com.arny.allfy.domain.usecase.User

class UserUseCases(
    val getUserDetails: GetUserDetails,
    val setUserDetails: SetUserDetails,
    val followUser: FollowUserUseCase,
    val unfollowUser: UnfollowUserUseCase
)