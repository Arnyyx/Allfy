package com.arny.allfy.domain.usecase.user

class UserUseCases(
    val getUserDetails: GetUserDetails,
    val setUserDetails: SetUserDetails,
    val getUsersByIDs: GetUsersByIDs,
    val followUser: FollowUser,
    val unfollowUser: UnfollowUser,
    val getFollowers: GetFollowers,
    val getFollowingCount: GetFollowingCount,
    val getFollowersCount: GetFollowersCount,
    val getPostIds: GetPostIds,
    val checkIfFollowing: CheckIfFollowing
)