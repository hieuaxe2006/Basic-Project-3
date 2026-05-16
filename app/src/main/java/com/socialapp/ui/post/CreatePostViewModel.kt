package com.socialapp.ui.post

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.model.User
import com.socialapp.data.repository.PostRepository
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch

data class CreatePostState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val imageUri: Uri? = null,
    val selectedTags: List<String> = emptyList(),
    val backgroundColor: String = "",
    val codeSnippet: String = "",
    val language: String = "Kotlin",
    val friends: List<User> = emptyList(),
    val selectedTaggedUsers: List<User> = emptyList(),
    val isLoadingFriends: Boolean = false
)

class CreatePostViewModel : ViewModel() {
    private val repo = PostRepository()
    private val socialRepo = SocialRepository()

    var state by mutableStateOf(CreatePostState())
        private set

    fun setImageUri(uri: Uri?) { state = state.copy(imageUri = uri) }
    fun removeImage() { state = state.copy(imageUri = null) }
    fun updateCode(code: String, lang: String) { state = state.copy(codeSnippet = code, language = lang) }

    fun toggleTag(tag: String) {
        val current = state.selectedTags.toMutableList()
        if (tag in current) current.remove(tag) else current.add(tag)
        state = state.copy(selectedTags = current)
    }

    fun loadFriends() {
        viewModelScope.launch {
            state = state.copy(isLoadingFriends = true)
            socialRepo.getFriends().onSuccess {
                state = state.copy(friends = it, isLoadingFriends = false)
            }.onFailure {
                state = state.copy(isLoadingFriends = false)
            }
        }
    }

    fun toggleTagUser(user: User) {
        val current = state.selectedTaggedUsers.toMutableList()
        if (current.any { it.id == user.id }) {
            current.removeAll { it.id == user.id }
        } else {
            current.add(user)
        }
        state = state.copy(selectedTaggedUsers = current)
    }

    fun createPost(content: String, context: Context) {
        if (content.isBlank() && state.imageUri == null && state.codeSnippet.isBlank()) return
        if (state.selectedTags.isEmpty()) {
            state = state.copy(error = "Vui lòng chọn ít nhất một chủ đề")
            return
        }

        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            val base64 = state.imageUri?.let { uri ->
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                } catch (e: Exception) { null }
            }

            repo.createPost(
                content = content,
                imageBase64 = base64,
                tags = state.selectedTags,
                backgroundColor = state.backgroundColor,
                codeSnippet = state.codeSnippet,
                language = state.language,
                taggedUserIds = state.selectedTaggedUsers.map { it.id }
            ).onSuccess {
                state = state.copy(isLoading = false, isSuccess = true)
            }.onFailure {
                state = state.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun reset() { state = CreatePostState() }
}