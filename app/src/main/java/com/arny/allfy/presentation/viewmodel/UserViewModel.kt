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
//                userUseCases.getUserByID(userID).collect {
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
    auth: FirebaseAuth
) : ViewModel() {
    private val userID = auth.currentUser?.uid

    private val _getUserData = mutableStateOf<Response<User>>(Response.Loading)
    val getUserData: State<Response<User>> = _getUserData

    private val _getCurrentUser = mutableStateOf<Response<User>>(Response.Loading)
    val getCurrentUser: State<Response<User>> = _getCurrentUser

    fun getCurrentUser() {
        viewModelScope.launch {
            if (userID != null) {
                viewModelScope.launch {
                    userUseCases.getUserDetails(userID).collect {
                        _getCurrentUser.value = it
                    }
                }
            }
        }
    }

    private val _updateProfileStatus = mutableStateOf<Response<Boolean>>(Response.Success(false))
    val updateProfileStatus: State<Response<Boolean>> = _updateProfileStatus

    fun updateUserProfile(updatedUser: User, imageUri: Uri?) {
        viewModelScope.launch {
            userUseCases.setUserDetails(updatedUser, imageUri).collect {
                _updateProfileStatus.value = it
            }
        }
    }

    fun getUser(userID: String) {
        viewModelScope.launch {
            userUseCases.getUserDetails(userID).collect {
                _getUserData.value = it
            }
        }
    }
}
