package com.socialapp.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.repository.AuthRepository
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()

    var state by mutableStateOf(AuthState(isLoggedIn = repo.currentUser != null))
        private set

    fun login(email: String, password: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            repo.login(email, password)
                .onSuccess { state = state.copy(isLoading = false, isLoggedIn = true) }
                .onFailure { state = state.copy(isLoading = false, error = it.message) }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            repo.register(username, email, password)
                .onSuccess { state = state.copy(isLoading = false, isLoggedIn = true) }
                .onFailure { state = state.copy(isLoading = false, error = it.message) }
        }
    }

    fun logout() {
        repo.logout()
        state = AuthState()
    }

    fun clearError() {
        state = state.copy(error = null)
    }
}
