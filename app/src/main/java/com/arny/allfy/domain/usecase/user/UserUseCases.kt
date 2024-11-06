package com.arny.allfy.domain.usecase.user

class UserUseCases(
    val getUserDetails: GetUserDetails,
    val setUserDetails: SetUserDetails,
    val followUser: FollowUserUseCase,
    val unfollowUser: UnfollowUserUseCase,
    val getFollowers: GetFollowersUseCase
)