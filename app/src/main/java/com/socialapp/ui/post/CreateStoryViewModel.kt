package com.socialapp.ui.post

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import com.socialapp.data.model.Story
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID

data class CreateStoryState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val textContent: String = "",
    val backgroundColor: String = "#1877F2",
    val selectedImageUri: Uri? = null,
    val visibility: String = "public" // "public", "friends", "private"
)

class CreateStoryViewModel : ViewModel() {
    private val repo = SocialRepository()
    private val storage = FirebaseStorage.getInstance()
    
    var state by mutableStateOf(CreateStoryState())
        private set

    fun updateText(text: String) {
        state = state.copy(textContent = text)
    }

    fun updateColor(color: String) {
        state = state.copy(backgroundColor = color)
    }

    fun updateImage(uri: Uri?) {
        state = state.copy(selectedImageUri = uri)
    }

    fun updateVisibility(visibility: String) {
        state = state.copy(visibility = visibility)
    }

    fun createStory() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val uid = repo.currentUid ?: throw Exception("Not logged in")
                val calendar = Calendar.getInstance()
                val createdAt = Timestamp.now()
                calendar.time = createdAt.toDate()
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val expiresAt = Timestamp(calendar.time)

                var imageUrl = ""
                val uri = state.selectedImageUri
                if (uri != null) {
                    val ref = storage.reference.child("stories/${UUID.randomUUID()}")
                    ref.putFile(uri).await()
                    imageUrl = ref.downloadUrl.await().toString()
                }

                val type = if (imageUrl.isNotBlank()) "image" else "text"

                val story = Story(
                    userId = uid,
                    imageUrl = imageUrl,
                    text = state.textContent,
                    backgroundColor = state.backgroundColor,
                    type = type,
                    visibility = state.visibility,
                    createdAt = createdAt,
                    expiresAt = expiresAt
                )

                repo.saveStory(story).onSuccess {
                    state = state.copy(isLoading = false, isSuccess = true)
                }.onFailure {
                    state = state.copy(isLoading = false, error = it.message)
                }
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun reset() {
        state = CreateStoryState()
    }
}
