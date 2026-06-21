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
import com.socialapp.data.repository.UserRepository
import kotlinx.coroutines.launch

data class CreatePostState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val currentUser: User? = null, // Thêm để lưu thông tin người đăng
    val selectedImages: List<Uri> = emptyList(),
    val selectedTags: List<String> = emptyList(),
    val backgroundColor: String = "",
    val codeSnippet: String = "",
    val language: String = "Push",
    val friends: List<User> = emptyList(),
    val selectedTaggedUsers: List<User> = emptyList(),
    val isLoadingFriends: Boolean = false,
    val aiSuggestion: String? = null,
    val isAnalyzingAi: Boolean = false,
    val isAutoTagging: Boolean = false
)

class CreatePostViewModel : ViewModel() {
    private val repo = PostRepository()
    private val socialRepo = SocialRepository()
    private val userRepo = UserRepository()

    var state by mutableStateOf(CreatePostState())
        private set

    init {
        loadCurrentUser() // Tải thông tin user ngay khi mở màn hình
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val uid = repo.currentUid ?: return@launch
            userRepo.getUser(uid).onSuccess {
                state = state.copy(currentUser = it)
            }
        }
    }

    fun addImages(uris: List<Uri>) {
        state = state.copy(selectedImages = state.selectedImages + uris)
    }

    fun removeImage(uri: Uri) {
        state = state.copy(selectedImages = state.selectedImages.filter { it != uri })
    }

    fun updateCode(code: String, lang: String) {
        state = state.copy(codeSnippet = code, language = lang)
    }

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
            }.onFailure { state = state.copy(isLoadingFriends = false) }
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
        if (content.isBlank() && state.selectedImages.isEmpty() && state.codeSnippet.isBlank()) return
        if (state.selectedTags.isEmpty()) {
            state = state.copy(error = "Vui lòng chọn ít nhất một chủ đề")
            return
        }

        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            val imagesBase64 = state.selectedImages.mapNotNull { uri ->
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                } catch (e: Exception) { null }
            }

            repo.createPost(
                content = content,
                imagesBase64 = imagesBase64,
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

    fun getAiSuggestion(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            state = state.copy(isAnalyzingAi = true)
            val prompt = "Phân tích bài tập thể hình này: \"$content\""
            com.socialapp.data.remote.GeminiApi.generateContent(prompt).onSuccess { text ->
                state = state.copy(aiSuggestion = text, isAnalyzingAi = false)
            }.onFailure { state = state.copy(isAnalyzingAi = false) }
        }
    }

    fun autoTagContent(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            state = state.copy(isAutoTagging = true)
            val prompt = "Chọn tag phù hợp nhất cho bài viết: \"$content\""
            com.socialapp.data.remote.GeminiApi.generateContent(prompt).onSuccess { tagResult ->
                val cleanTag = tagResult.trim().removeSurrounding("\"")
                val validTags = listOf("Workout", "Nutrition", "Supplements", "Transformation", "Motivation", "Q&A")
                val matchedTag = validTags.firstOrNull { it.equals(cleanTag, ignoreCase = true) }
                if (matchedTag != null) {
                    val current = state.selectedTags.toMutableList()
                    if (matchedTag !in current) current.add(matchedTag)
                    state = state.copy(selectedTags = current)
                }
                state = state.copy(isAutoTagging = false)
            }.onFailure { state = state.copy(isAutoTagging = false) }
        }
    }

    fun reset() { state = CreatePostState() }
}