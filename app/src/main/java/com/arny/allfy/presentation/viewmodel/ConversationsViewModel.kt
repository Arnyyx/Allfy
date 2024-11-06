package com.arny.allfy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arny.allfy.domain.usecase.conversation.GetConversationsUseCase
import com.arny.allfy.presentation.state.ConversationsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val getConversationsUseCase: GetConversationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConversationsUiState>(ConversationsUiState.Initial)
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            getConversationsUseCase()
                .catch { error ->
                    _uiState.value = ConversationsUiState.Error(
                        error.message ?: "Failed to load conversations"
                    )
                }
                .collect { conversations ->
                    _uiState.value = ConversationsUiState.Success(conversations)
                }
        }
    }
}