package com.arny.allfy.domain.usecase.user

class UserUseCases(
    val getUserDetails: GetUserDetails,
    val setUserDetailsUseCase: SetUserDetailsUseCase,
    val followUser: FollowUserUseCase,
    val unfollowUser: UnfollowUserUseCase,
    val getUsersByIDs: GetUsersByIDsUseCase,
    val getFollowersFromSubcollection: GetFollowersFromSubcollectionUseCase,
    val getFollowingCount: GetFollowingCountUseCase,
    val getFollowersCount: GetFollowersCountUseCase,
    val getPostsIdsFromSubcollection: GetPostsIdsFromSubcollectionUseCase,
    val checkIfFollowing: CheckIfFollowingUseCase
)