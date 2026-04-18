package com.socialapp.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.model.User
import com.socialapp.data.repository.UserRepository
import kotlinx.coroutines.launch

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

class ProfileViewModel : ViewModel() {
    private val repo = UserRepository()

    var state by mutableStateOf(ProfileState())
        private set

    fun loadProfile(uid: String? = null) {
        val targetUid = uid ?: repo.currentUid ?: return
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            repo.getUser(targetUid)
                .onSuccess { state = state.copy(isLoading = false, user = it) }
                .onFailure { state = state.copy(isLoading = false, error = it.message) }
        }
    }

    fun toggleEdit() {
        state = state.copy(isEditing = !state.isEditing)
    }

    fun saveProfile(username: String, bio: String) {
        val uid = state.user?.id ?: return
        viewModelScope.launch {
            state = state.copy(isSaving = true)
            repo.updateProfile(uid, username, bio, null)
                .onSuccess {
                    state = state.copy(
                        isSaving = false,
                        isEditing = false,
                        user = state.user?.copy(username = username, bio = bio)
                    )
                }
                .onFailure { state = state.copy(isSaving = false, error = it.message) }
        }
    }

    val isOwnProfile: Boolean
        get() = state.user?.id == repo.currentUid
}
