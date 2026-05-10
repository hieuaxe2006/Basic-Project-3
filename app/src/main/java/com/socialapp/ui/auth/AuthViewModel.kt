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
    val isLoggedIn: Boolean = false,
    val userRole: String? = null,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val repo = AuthRepository()
    private val db = FirebaseFirestore.getInstance()

    var state by mutableStateOf(AuthState(isLoggedIn = repo.currentUser != null))
        private set

    init {
        if (repo.currentUser != null) {
            fetchUserRole(repo.currentUser!!.uid)
        }
    }

    private fun fetchUserRole(uid: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                val role = doc.getString("role") ?: "user"
                state = state.copy(isLoggedIn = true, userRole = role)
            } catch (e: Exception) {
                state = state.copy(error = e.message)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            repo.login(email, password)
                .onSuccess {
                    val uid = repo.currentUser?.uid
                    if (uid != null) {
                        val doc = db.collection("users").document(uid).get().await()
                        val role = doc.getString("role") ?: "user"
                        state = state.copy(isLoading = false, isLoggedIn = true, userRole = role)
                    }
                }
                .onFailure { state = state.copy(isLoading = false, error = it.message) }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            repo.register(username, email, password)
                .onSuccess { state = state.copy(isLoading = false, isLoggedIn = true, userRole = "user") }
                .onFailure { state = state.copy(isLoading = false, error = it.message) }
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