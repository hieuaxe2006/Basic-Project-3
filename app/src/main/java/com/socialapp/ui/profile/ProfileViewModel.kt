package com.socialapp.ui.profile

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.model.Post
import com.socialapp.data.model.User
import com.socialapp.data.remote.ImgBBApi
import com.socialapp.data.repository.SocialRepository
import com.socialapp.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val followersList: List<User> = emptyList(),
    val followingList: List<User> = emptyList(),
    val isListLoading: Boolean = false,
    val postedPosts: List<Post> = emptyList(),
    val savedPosts: List<Post> = emptyList(),
    val isPostsLoading: Boolean = false,
    val isFollowing: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val isChangingPassword: Boolean = false,
    val passwordChangeSuccess: Boolean = false,
    val passwordChangeError: String? = null
)

class ProfileViewModel : ViewModel() {
    private val repo = UserRepository()
    private val socialRepo = SocialRepository()

    var state by mutableStateOf(ProfileState())
        private set

    init {
        observeSavedPosts()
    }

    private var profileJob: Job? = null

    fun loadProfile(uid: String? = null) {
        val targetUid = uid ?: repo.currentUid ?: return
        profileJob?.cancel()
        state = state.copy(isLoading = true, error = null)
        profileJob = viewModelScope.launch {
            repo.getUserSnapshot(targetUid)
                .catch { e -> state = state.copy(isLoading = false, error = e.message) }
                .collect { user ->
                    state = state.copy(isLoading = false, user = user)
                    loadPostedPosts(user.id)
                    if (user.id == repo.currentUid) {
                        loadSavedPosts()
                    } else {
                        checkFollowingStatus(user.id)
                    }
                }
        }
    }

    private fun checkFollowingStatus(targetUid: String) {
        viewModelScope.launch {
            val following = socialRepo.isFollowing(targetUid)
            state = state.copy(isFollowing = following)
        }
    }

    fun toggleFollow() {
        val targetUid = state.user?.id ?: return
        viewModelScope.launch {
            socialRepo.toggleFollow(targetUid).onSuccess {
                state = state.copy(isFollowing = it)
            }
        }
    }

    fun loadPostedPosts(uid: String) {
        viewModelScope.launch {
            state = state.copy(isPostsLoading = true)
            socialRepo.getUserPosts(uid)
                .onSuccess { state = state.copy(isPostsLoading = false, postedPosts = it) }
                .onFailure { state = state.copy(isPostsLoading = false) }
        }
    }

    fun observeSavedPosts() {
        viewModelScope.launch {
            socialRepo.getSavedPostIdsFlow().collect {
                if (isOwnProfile) {
                    loadSavedPosts()
                }
            }
        }
    }

    fun loadSavedPosts() {
        viewModelScope.launch {
            socialRepo.getSavedPosts()
                .onSuccess { state = state.copy(isPostsLoading = false, savedPosts = it) }
                .onFailure { state = state.copy(isPostsLoading = false) }
        }
    }

    fun loadFollowers() {
        val targetUid = state.user?.id ?: return
        viewModelScope.launch {
            state = state.copy(isListLoading = true)
            repo.getFollowers(targetUid)
                .onSuccess { state = state.copy(isListLoading = false, followersList = it) }
                .onFailure { state = state.copy(isListLoading = false) }
        }
    }

    fun loadFollowing() {
        val targetUid = state.user?.id ?: return
        viewModelScope.launch {
            state = state.copy(isListLoading = true)
            repo.getFollowing(targetUid)
                .onSuccess { state = state.copy(isListLoading = false, followingList = it) }
                .onFailure { state = state.copy(isListLoading = false) }
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

    fun uploadAvatar(uri: Uri, context: Context) {
        val uid = state.user?.id ?: return
        viewModelScope.launch {
            state = state.copy(isUploadingAvatar = true)
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                val base64 = bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                    ?: throw Exception("Failed to read image")
                val url = ImgBBApi.uploadImage(base64).getOrThrow()
                repo.updateAvatar(uid, url)
                    .onSuccess {
                        state = state.copy(
                            isUploadingAvatar = false,
                            user = state.user?.copy(avatar = url)
                        )
                    }
                    .onFailure { state = state.copy(isUploadingAvatar = false, error = it.message) }
            } catch (e: Exception) {
                state = state.copy(isUploadingAvatar = false, error = e.message)
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            state = state.copy(isChangingPassword = true, passwordChangeError = null, passwordChangeSuccess = false)
            repo.changePassword(currentPassword, newPassword)
                .onSuccess {
                    state = state.copy(isChangingPassword = false, passwordChangeSuccess = true)
                }
                .onFailure {
                    state = state.copy(isChangingPassword = false, passwordChangeError = it.message)
                }
        }
    }

    fun clearPasswordState() {
        state = state.copy(passwordChangeSuccess = false, passwordChangeError = null)
    }

    val isOwnProfile: Boolean
        get() = state.user?.id == repo.currentUid

    val isFollowing: Boolean
        get() = state.isFollowing
}
