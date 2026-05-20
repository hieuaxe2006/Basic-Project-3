package com.socialapp.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.model.Post
import com.socialapp.data.model.User
import com.socialapp.data.repository.AdminRepository
import kotlinx.coroutines.launch

data class AdminState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val posts: List<Post> = emptyList(),
    val revenue: Long = 0,
    val topUsers: List<User> = emptyList(),
    val error: String? = null
)

class AdminViewModel : ViewModel() {
    private val repo = AdminRepository()

    var state by mutableStateOf(AdminState())
        private set

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                val users = repo.getAllUsers()
                val posts = repo.getAllPosts()
                val revenue = repo.getRevenueStats()
                val topUsers = repo.getTopFollowers()
                state = state.copy(
                    isLoading = false,
                    users = users,
                    posts = posts,
                    revenue = revenue,
                    topUsers = topUsers
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleBlockUser(uid: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repo.toggleBlockUser(uid, !currentStatus).onSuccess {
                loadData()
            }.onFailure {
                state = state.copy(error = it.message)
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repo.deletePost(postId).onSuccess {
                loadData()
            }.onFailure {
                state = state.copy(error = it.message)
            }
        }
    }

    fun approvePost(postId: String) {
        viewModelScope.launch {
            repo.approvePost(postId).onSuccess {
                loadData()
            }.onFailure {
                state = state.copy(error = it.message)
            }
        }
    }

    fun rejectPost(postId: String) {
        viewModelScope.launch {
            repo.rejectPost(postId).onSuccess {
                loadData()
            }.onFailure {
                state = state.copy(error = it.message)
            }
        }
    }
}
