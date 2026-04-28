package com.socialapp.ui.admin

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.model.Post
import com.socialapp.data.model.User
import com.socialapp.data.repository.AdminRepository
import kotlinx.coroutines.launch

data class AdminState(
    val users: List<User> = emptyList(),
    val posts: List<Post> = emptyList(),
    val revenue: Long = 0,
    val topUsers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AdminViewModel : ViewModel() {
    private val repo = AdminRepository()
    var state by mutableStateOf(AdminState())
        private set

    init { loadAdminData() }

    fun loadAdminData() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                val users = repo.getAllUsers()
                val posts = repo.getAllPosts()
                val rev = repo.getRevenueStats()
                val top = repo.getTopFollowers()
                state = state.copy(users = users, posts = posts, revenue = rev, topUsers = top, isLoading = false)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repo.deletePost(postId).onSuccess { loadAdminData() }
        }
    }

    fun toggleBlockUser(uid: String, currentBlockStatus: Boolean) {
        viewModelScope.launch {
            repo.toggleBlockUser(uid, !currentBlockStatus).onSuccess { loadAdminData() }
        }
    }
}