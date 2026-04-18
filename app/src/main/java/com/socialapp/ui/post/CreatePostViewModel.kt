package com.socialapp.ui.post

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.repository.PostRepository
import kotlinx.coroutines.launch

data class CreatePostState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val imageUri: Uri? = null
)

class CreatePostViewModel : ViewModel() {
    private val repo = PostRepository()

    var state by mutableStateOf(CreatePostState())
        private set

    fun setImageUri(uri: Uri?) {
        state = state.copy(imageUri = uri)
    }

    fun removeImage() {
        state = state.copy(imageUri = null)
    }

    fun createPost(content: String, context: Context) {
        if (content.isBlank() && state.imageUri == null) return

        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)

            val base64 = state.imageUri?.let { uri ->
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                } catch (e: Exception) {
                    null
                }
            }

            repo.createPost(content, base64)
                .onSuccess { state = state.copy(isLoading = false, isSuccess = true) }
                .onFailure { state = state.copy(isLoading = false, error = it.message) }
        }
    }

    fun reset() {
        state = CreatePostState()
    }
}
