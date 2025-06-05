package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.usecase.user.UserUseCases
import com.arny.allfy.presentation.state.UserState
import com.arny.allfy.utils.Response
import com.arny.allfy.utils.mapSuccess
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
                _userState.update { it.copy(currentUserState = response) }
            }
        }
    }

    fun getUserDetails(userId: String) {
        viewModelScope.launch {
            userUseCases.getUserDetails(userId).collect { response ->
                _userState.update { it.copy(otherUserState = response) }
            }
        }
    }

    fun updateUserProfile(updatedUser: User, imageUri: Uri?) {
        viewModelScope.launch {
            userUseCases.setUserDetails(updatedUser, imageUri).collect { response ->
                _userState.update {
                    it.copy(updateProfileState = response.mapSuccess { true })
                }
            }
        }
    }

    fun followUser(currentUserId: String, targetUserId: String) {
        viewModelScope.launch {
            userUseCases.followUser(currentUserId, targetUserId).collect { response ->
                _userState.update {
                    it.copy(followUserState = response.mapSuccess { true })
                }
            }
        }
    }

    fun unfollowUser(currentUserId: String, targetUserId: String) {
        viewModelScope.launch {
            userUseCases.unfollowUser(currentUserId, targetUserId).collect { response ->
                _userState.update {
                    it.copy(unfollowUserState = response.mapSuccess { true })
                }
            }
        }
    }

    fun getFollowers(userId: String) {
        viewModelScope.launch {
            userUseCases.getFollowers(userId).collect { response ->
                _userState.update { it.copy(followersState = response) }
            }
        }
    }

    fun getFollowings(userId: String) {
        viewModelScope.launch {
            userUseCases.getFollowings(userId).collect { response ->
                _userState.update { it.copy(followingsState = response) }
            }
        }
    }

    fun getFollowingCount(userId: String) {
        viewModelScope.launch {
            userUseCases.getFollowingCount(userId).collect { response ->
                _userState.update { it.copy(followingCountState = response) }
            }
        }
    }

    fun getFollowersCount(userId: String) {
        viewModelScope.launch {
            userUseCases.getFollowersCount(userId).collect { response ->
                _userState.update { it.copy(followersCountState = response) }
            }
        }
    }

    fun getPostIds(userId: String) {
        viewModelScope.launch {
            userUseCases.getPostIds(userId).collect { response ->
                _userState.update { it.copy(postsIdsState = response) }
            }
        }
    }

    fun checkIfFollowing(currentUserId: String, targetUserId: String) {
        viewModelScope.launch {
            userUseCases.checkIfFollowing(currentUserId, targetUserId).collect { response ->
                _userState.update { it.copy(checkIfFollowingState = response) }
            }
        }
    }

    fun getUsersByIDs(userIDs: List<String>) {
        viewModelScope.launch {
            userUseCases.getUsersByIDs(userIDs).collect { response ->
                _userState.update { it.copy(usersState = response) }
            }
        }
    }

    fun resetCurrentUserState() {
        _userState.update { it.copy(currentUserState = Response.Idle) }
    }

    fun resetOtherUserState() {
        _userState.update { it.copy(otherUserState = Response.Idle) }
    }

    fun resetUpdateProfileState() {
        _userState.update { it.copy(updateProfileState = Response.Idle) }
    }

    fun resetFollowUserState() {
        _userState.update { it.copy(followUserState = Response.Idle) }
    }

    fun resetUnfollowUserState() {
        _userState.update { it.copy(unfollowUserState = Response.Idle) }
    }

    fun resetFollowersState() {
        _userState.update { it.copy(followersState = Response.Idle) }
    }

    fun resetFollowingsState() {
        _userState.update { it.copy(followingsState = Response.Idle) }
    }

    fun resetFollowingCountState() {
        _userState.update { it.copy(followingCountState = Response.Idle) }
    }

    fun resetFollowersCountState() {
        _userState.update { it.copy(followersCountState = Response.Idle) }
    }

    fun resetPostsIdsState() {
        _userState.update { it.copy(postsIdsState = Response.Idle) }
    }

    fun resetCheckIfFollowingState() {
        _userState.update { it.copy(checkIfFollowingState = Response.Idle) }
    }

    fun resetUsersState() {
        _userState.update { it.copy(usersState = Response.Idle) }
    }

    fun clearUserState() {
        _userState.value = UserState()
    }
}