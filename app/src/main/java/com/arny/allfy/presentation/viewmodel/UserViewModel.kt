package com.arny.allfy.presentation.viewmodel

//import android.net.Uri
//import androidx.compose.runtime.State
//import androidx.compose.runtime.mutableStateOf
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.arny.allfy.domain.model.User
//import com.arny.allfy.domain.usecase.User.UserUseCases
//import com.arny.allfy.utils.Response
//import com.google.firebase.auth.FirebaseAuth
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//@HiltViewModel
//class UserViewModel @Inject constructor(
//    auth: FirebaseAuth,
//    private val userUseCases: UserUseCases
//) : ViewModel() {
//    private val userID = auth.currentUser?.uid
//    private val _getUserData = mutableStateOf<Response<User?>>(Response.Success(null))
//    val getUserData: State<Response<User?>> = _getUserData
//
//    private val _setUserData = mutableStateOf<Response<Boolean>>(Response.Success(false))
//    val setUserData: State<Response<Boolean>> = _setUserData
//
//    fun getUserInfo() {
//        if (userID != null) {
//            viewModelScope.launch {
//                userUseCases.getUserDetails(userID).collect {
//                    _getUserData.value = it
//                }
//            }
//        }
//    }
//
//    fun setUserInfo(name: String, userName: String, bio: String, imageUri: Uri) {
//        if (userID != null) {
//            viewModelScope.launch {
//                userUseCases.setUserDetails(userID, name, userName, bio, imageUri).collect {
//                    _setUserData.value = it
//                }
//            }
//        }
//    }
//}

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.repository.UserRepository
import com.arny.allfy.domain.usecase.User.UserUseCases
import com.arny.allfy.utils.Response
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.State

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userUseCases: UserUseCases,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val userID = auth.currentUser?.uid

    private val _getUserData = mutableStateOf<Response<User>>(Response.Loading)
    val getUserData: State<Response<User>> = _getUserData

    private val _updateProfileStatus = MutableStateFlow<Response<Boolean>>(Response.Success(false))
    val updateProfileStatus: StateFlow<Response<Boolean>> = _updateProfileStatus

    fun getUserInfo() {
        viewModelScope.launch {
            if (userID != null) {
                viewModelScope.launch {
                    userUseCases.getUserDetails(userID).collect {
                        _getUserData.value = it
                    }
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

//    fun updateProfilePicture(imageUri: String) {
//        viewModelScope.launch {
//            _updateProfileStatus.value = Response.Loading
//            try {
//                val result = userRepository.updateProfilePicture(imageUri)
//                if (result) {
//                    _updateProfileStatus.value = Response.Success(true)
//                    // Refresh user data after successful update
//                    getUserInfo()
//                } else {
//                    _updateProfileStatus.value =
//                        Response.Error("Failed to update profile picture")
//                }
//            } catch (e: Exception) {
//                _updateProfileStatus.value =
//                    Response.Error(e.message ?: "An unknown error occurred")
//            }
//        }
//    }
}
