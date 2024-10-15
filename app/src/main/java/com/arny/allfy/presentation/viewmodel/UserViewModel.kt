package com.arny.allfy.presentation.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.model.User
import com.arny.allfy.domain.usecase.User.UserUseCases
import com.arny.allfy.utils.Response
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    auth: FirebaseAuth,
    private val userUseCases: UserUseCases
) : ViewModel() {
    private val userID = auth.currentUser?.uid
    private val _getUserData = mutableStateOf<Response<User?>>(Response.Success(null))
    val getUserData: State<Response<User?>> = _getUserData

    private val _setUserData = mutableStateOf<Response<Boolean>>(Response.Success(false))
    val setUserData: State<Response<Boolean>> = _setUserData

    fun getUserInfo() {
        if (userID != null) {
            viewModelScope.launch {
                userUseCases.getUserDetails(userID).collect {
                    _getUserData.value = it
                }
            }
        }
    }

    fun setUserInfo(name: String, userName: String, bio: String) {
        if (userID != null) {
            viewModelScope.launch {
                userUseCases.setUserDetails(userID, name, userName, bio).collect {
                    _setUserData.value = it
                }
            }
        }
    }
}