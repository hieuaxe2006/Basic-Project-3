package com.socialapp.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.socialapp.data.repository.AuthRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthState(
    val isLoading: Boolean = false,
    val isSessionLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val userRole: String? = null,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()
    private val db = FirebaseFirestore.getInstance()

    var state by mutableStateOf(
        AuthState(isSessionLoading = repo.currentUser != null)
    )
        private set

    init {
        if (repo.currentUser != null) {
            fetchUserRole(repo.currentUser!!.uid)
        }
    }

    private fun fetchUserRole(uid: String) {
        viewModelScope.launch {
            try {
                val doc = kotlinx.coroutines.withTimeout(8000L) {
                    db.collection("users").document(uid).get().await()
                }
                val role = doc.getString("role") ?: "user"
                state = state.copy(isLoggedIn = true, userRole = role, isSessionLoading = false)
            } catch (e: Exception) {
                // Phiên hết hạn hoặc Firestore lỗi -> đưa về login
                repo.logout()
                state = state.copy(
                    isLoggedIn = false,
                    isSessionLoading = false,
                    error = "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."
                )
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            repo.login(email, password)
                .onSuccess { user ->
                    state = state.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        userRole = user.role
                    )
                }
                .onFailure { state = state.copy(isLoading = false, error = it.message ?: "Đăng nhập thất bại. Vui lòng kiểm tra kết nối mạng hoặc thông tin đăng nhập.") }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            repo.register(username, email, password)
                .onSuccess { state = state.copy(isLoading = false, isLoggedIn = true, userRole = "user") }
                .onFailure { state = state.copy(isLoading = false, error = it.message ?: "Đăng ký thất bại. Vui lòng kiểm tra kết nối mạng.") }
        }
    }

    fun logout() {
        repo.logout()
        state = AuthState(isLoggedIn = false, userRole = null)
    }

    fun clearError() {
        state = state.copy(error = null)
    }
}