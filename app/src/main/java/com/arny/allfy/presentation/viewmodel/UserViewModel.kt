package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.usecase.user.UserUseCases
import com.arny.allfy.utils.Response
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.State
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

    private val _updateProfileStatus = mutableStateOf<Response<Boolean>>(Response.Success(false))
    val updateProfileStatus: State<Response<Boolean>> = _updateProfileStatus

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

    fun updateUserProfile(updatedUser: User, imageUri: Uri?) {
        viewModelScope.launch {
            userUseCases.setUserDetails(updatedUser, imageUri).collect {
                _updateProfileStatus.value = it
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

    fun isFollowingUser(userId: String): Boolean {
        return when (val user = _currentUser.value) {
            is Response.Success -> user.data.following.contains(userId)
            else -> false
        }
    }

    private val _followers = MutableStateFlow<Response<List<User>>>(Response.Loading)
    val followers: StateFlow<Response<List<User>>> = _followers.asStateFlow()

    fun getFollowers(followerId: List<String>) {
        viewModelScope.launch {
            userUseCases.getFollowers(followerId)
                .collect { response ->
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
                val users = userUseCases.getUsersByIDs(userIDs).collect { response ->
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

}