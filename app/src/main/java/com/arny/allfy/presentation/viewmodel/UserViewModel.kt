package com.arny.allfy.presentation.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.usecase.User.UserUseCases
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

    private val _user = MutableStateFlow<Response<User>>(Response.Loading)
    val user: StateFlow<Response<User>> = _user.asStateFlow()

    fun getUserByID(userID: String?) {
        viewModelScope.launch {
            if (userID != null) {
                viewModelScope.launch {
                    userUseCases.getUserDetails(userID).collect {
                        _user.value = it
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
}
