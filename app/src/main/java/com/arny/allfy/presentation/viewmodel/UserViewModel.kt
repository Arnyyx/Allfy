package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.usecase.user.UserUseCases
import com.arny.allfy.utils.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
) : ViewModel() {
    private val _userState = MutableStateFlow(UserState())
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    fun getCurrentUser(userId: String) {
        viewModelScope.launch {
            userUseCases.getUserDetails(userId).collect { response ->
                when (response) {
                    is Response.Loading -> _userState.update { it.copy(isLoadingCurrentUser = true) }
                    is Response.Error -> _userState.update { it.copy(errorCurrentUser = response.message) }
                    is Response.Success -> _userState.update {
                        it.copy(
                            currentUser = response.data,
                            isLoadingCurrentUser = false
                        )
                    }
                }
            }
        }
    }

    fun getUserDetails(userId: String) {
        viewModelScope.launch {
            userUseCases.getUserDetails(userId).collect { response ->
                when (response) {
                    is Response.Loading -> _userState.update { it.copy(isLoadingOtherUser = true) }
                    is Response.Error -> _userState.update { it.copy(errorOtherUser = response.message) }
                    is Response.Success -> _userState.update {
                        it.copy(
                            otherUser = response.data,
                            isLoadingOtherUser = false
                        )
                    }
                }
            }
        }
    }

    fun updateUserProfile(updatedUser: User, imageUri: Uri?) {
        viewModelScope.launch {
            userUseCases.setUserDetails(updatedUser, imageUri).collect { response ->
                when (response) {
                    is Response.Loading -> _userState.update { it.copy(isLoadingUpdateProfile = true) }
                    is Response.Error -> _userState.update { it.copy(updateProfileError = response.message) }
                    is Response.Success -> _userState.update { it.copy(isLoadingUpdateProfile = false) }
                }
            }
        }
    }

    fun followUser(currentUserId: String, targetUserId: String) {
        viewModelScope.launch {
            userUseCases.followUser(currentUserId, targetUserId).collect { response ->
                when (response) {
                    is Response.Loading -> _userState.update { it.copy(isLoadingFollowUser = true) }
                    is Response.Error -> _userState.update { it.copy(followUserError = response.message) }
                    is Response.Success -> _userState.update { it.copy(isLoadingFollowUser = false) }
                }
            }
        }
    }

    fun unfollowUser(currentUserId: String, targetUserId: String) {
        viewModelScope.launch {
            userUseCases.unfollowUser(currentUserId, targetUserId).collect { response ->
                when (response) {
                    is Response.Loading -> _userState.update { it.copy(isLoadingUnfollowUser = true) }
                    is Response.Error -> _userState.update { it.copy(unfollowUserError = response.message) }
                    is Response.Success -> _userState.update { it.copy(isLoadingUnfollowUser = false) }
                }
            }
        }
    }

    fun getFollowers(userId: String) {
        viewModelScope.launch {
            userUseCases.getFollowers(userId).collect { response ->
                when (response) {
                    is Response.Loading -> _userState.update { it.copy(isLoadingFollowers = true) }
                    is Response.Error -> _userState.update { it.copy(followersError = response.message) }
                    is Response.Success -> _userState.update {
                        it.copy(
                            followers = response.data,
                            isLoadingFollowers = false
                        )
                    }
                }
            }
        }
    }

    fun getFollowingCount(userId: String) {
        viewModelScope.launch {
            userUseCases.getFollowingCount(userId).collect { response ->
                when (response) {
                    is Response.Loading -> _userState.update { it.copy(isLoadingFollowingCount = true) }
                    is Response.Error -> _userState.update { it.copy(followingCountError = response.message) }
                    is Response.Success -> _userState.update {
                        it.copy(
                            followingCount = response.data,
                            isLoadingFollowingCount = false
                        )
                    }
                }
            }
        }
    }

    fun getFollowersCount(userId: String) {
        viewModelScope.launch {
            userUseCases.getFollowersCount(userId).collect { response ->
                when (response) {
                    is Response.Loading -> _userState.update { it.copy(isLoadingFollowersCount = true) }
                    is Response.Error -> _userState.update { it.copy(followersCountError = response.message) }
                    is Response.Success -> _userState.update {
                        it.copy(
                            followersCount = response.data,
                            isLoadingFollowersCount = false
                        )
                    }
                }
            }
        }
    }

    fun getPostIds(userId: String) {
        viewModelScope.launch {
            userUseCases.getPostIds(userId).collect { response ->
                when (response) {
                    is Response.Loading -> _userState.update { it.copy(isLoadingPostsIds = true) }
                    is Response.Error -> _userState.update { it.copy(postsIdsError = response.message) }
                    is Response.Success -> _userState.update {
                        it.copy(
                            postsIds = response.data,
                            isLoadingPostsIds = false
                        )
                    }
                }
            }
        }
    }

    fun checkIfFollowing(
        currentUserId: String,
        targetUserId: String
    ) {
        viewModelScope.launch {
            userUseCases.checkIfFollowing(currentUserId, targetUserId).collect { response ->
                when (response) {
                    is Response.Loading -> _userState.update { it.copy(isLoadingCheckIfFollowing = true) }
                    is Response.Error -> _userState.update { it.copy(checkIfFollowingError = response.message) }
                    is Response.Success -> _userState.update {
                        it.copy(
                            isFollowing = response.data,
                            isLoadingCheckIfFollowing = false
                        )
                    }
                }
            }
        }
    }

    fun getUsersByIDs(userIDs: List<String>) {
        viewModelScope.launch {
            userUseCases.getUsersByIDs(userIDs).collect { response ->
                when (response) {
                    is Response.Loading -> _userState.update { it.copy(isLoadingUsers = true) }
                    is Response.Error -> _userState.update { it.copy(usersError = response.message) }
                    is Response.Success -> _userState.update {
                        it.copy(
                            users = response.data,
                            isLoadingUsers = false
                        )
                    }
                }
            }
        }
    }

    fun clearUserState() {
        _userState.value = UserState()
    }
}

data class UserState(
    val isLoadingCurrentUser: Boolean = false,
    val currentUser: User = User(),
    val errorCurrentUser: String = "",

    val isLoadingOtherUser: Boolean = false,
    val otherUser: User = User(),
    val errorOtherUser: String = "",

    val isLoadingUpdateProfile: Boolean = false,
    val updateProfileError: String = "",

    val isLoadingFollowUser: Boolean = false,
    val followUserError: String = "",

    val isLoadingUnfollowUser: Boolean = false,
    val unfollowUserError: String = "",

    val isLoadingFollowers: Boolean = false,
    val followers: List<User> = emptyList(),
    val followersError: String = "",

    val isLoadingFollowingCount: Boolean = false,
    val followingCount: Int = 0,
    val followingCountError: String = "",

    val isLoadingFollowersCount: Boolean = false,
    val followersCount: Int = 0,
    val followersCountError: String = "",

    val isLoadingPostsIds: Boolean = false,
    val postsIds: List<String> = emptyList(),
    val postsIdsError: String = "",

    val isLoadingCheckIfFollowing: Boolean = false,
    val isFollowing: Boolean = false,
    val checkIfFollowingError: String = "",

    val isLoadingUsers: Boolean = false,
    val users: List<User> = emptyList(),
    val usersError: String = ""
)
