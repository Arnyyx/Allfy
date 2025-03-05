package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.usecase.user.UserUseCases
import com.arny.allfy.utils.Response
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
) : ViewModel() {
    private val _currentUser = MutableStateFlow<Response<User>>(Response.Loading)
    val currentUser: StateFlow<Response<User>> = _currentUser.asStateFlow()

    private val _otherUser = MutableStateFlow<Response<User>>(Response.Loading)
    val otherUser: StateFlow<Response<User>> = _otherUser.asStateFlow()

    private val _followStatus = MutableStateFlow<Response<Boolean>>(Response.Success(false))
    val followStatus: StateFlow<Response<Boolean>> = _followStatus.asStateFlow()

    fun getCurrentUser() {
        viewModelScope.launch {
            val userID = FirebaseAuth.getInstance().currentUser?.uid
            if (userID != null) {
                userUseCases.getUserDetails(userID).collect {
                    _currentUser.value = it
                }
            }
        }
    }

    fun getUserById(userId: String) {
        viewModelScope.launch {
            if (userId == FirebaseAuth.getInstance().currentUser?.uid) {
                getCurrentUser()
                _otherUser.value = _currentUser.value
            } else {
                userUseCases.getUserDetails(userId).collect {
                    _otherUser.value = it
                }
            }
        }
    }

    private val _updateProfileStatus = MutableStateFlow<Response<Boolean>>(Response.Success(false))
    val updateProfileStatus: StateFlow<Response<Boolean>> = _updateProfileStatus.asStateFlow()

    fun updateUserProfile(updatedUser: User, imageUri: Uri?) {
        viewModelScope.launch {
            _updateProfileStatus.value = Response.Loading
            userUseCases.setUserDetailsUseCase(updatedUser, imageUri).collect { response ->
                _updateProfileStatus.value = response
                if (response is Response.Success) {
                    getCurrentUser()
                    _updateProfileStatus.value = Response.Success(false)
                }
            }
        }
    }


    fun followUser(userId: String) {
        viewModelScope.launch {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId != null) {
                userUseCases.followUser(currentUserId, userId).collect { response ->
                    _followStatus.value = response
                    if (response is Response.Success && response.data) {
                        getCurrentUser()
                        getUserById(userId)
                    }
                }
            }
        }
    }

    fun unfollowUser(userId: String) {
        viewModelScope.launch {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId != null) {
                userUseCases.unfollowUser(currentUserId, userId).collect { response ->
                    _followStatus.value = response
                    if (response is Response.Success && response.data) {
                        getCurrentUser()
                        getUserById(userId)
                    }
                }
            }
        }
    }

    private val _followers = MutableStateFlow<Response<List<User>>>(Response.Loading)
    val followers: StateFlow<Response<List<User>>> = _followers.asStateFlow()

    fun getFollowersFromSubcollection(userId: String) {
        viewModelScope.launch {
            userUseCases.getFollowersFromSubcollection(userId).collect { response ->
                _followers.value = response
            }
        }
    }

    private val _users = MutableStateFlow<Response<List<User>>>(Response.Loading)
    val users: StateFlow<Response<List<User>>> = _users.asStateFlow()

    fun getUsers(userIDs: List<String>) {
        viewModelScope.launch {
            _users.value = Response.Loading
            try {
                userUseCases.getUsersByIDs(userIDs).collect { response ->
                    if (response is Response.Success) {
                        _users.value = Response.Success(response.data)
                    } else if (response is Response.Error) {
                        _users.value = Response.Error(response.message)
                    }
                }
            } catch (e: Exception) {
                _users.value = Response.Error(e.message ?: "Failed to fetch users")
            }
        }
    }

    private val _followingCount = MutableStateFlow<Response<Int>>(Response.Loading)
    val followingCount: StateFlow<Response<Int>> = _followingCount.asStateFlow()

    private val _followersCount = MutableStateFlow<Response<Int>>(Response.Loading)
    val followersCount: StateFlow<Response<Int>> = _followersCount.asStateFlow()

    private val _postsIds = MutableStateFlow<Response<List<String>>>(Response.Loading)
    val postsIds: StateFlow<Response<List<String>>> = _postsIds.asStateFlow()

    fun getFollowingCount(userId: String) {
        viewModelScope.launch {
            userUseCases.getFollowingCount(userId).collect { response ->
                _followingCount.value = response
            }
        }
    }

    fun getFollowersCount(userId: String) {
        viewModelScope.launch {
            userUseCases.getFollowersCount(userId).collect { response ->
                _followersCount.value = response
            }
        }
    }

    fun getPostsIdsFromSubcollection(userId: String) {
        viewModelScope.launch {
            userUseCases.getPostsIdsFromSubcollection(userId).collect { response ->
                _postsIds.value = response
            }
        }
    }

    fun checkIfFollowing(currentUserId: String, targetUserId: String): Flow<Response<Boolean>> {
        return userUseCases.checkIfFollowing(currentUserId, targetUserId)
    }

    fun clear() {
        _currentUser.value = Response.Loading
        _otherUser.value = Response.Loading
        _followStatus.value = Response.Success(false)
        _updateProfileStatus.value = Response.Loading
        _followers.value = Response.Loading
        _users.value = Response.Loading
    }
}