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
    val selectedUri: Uri? = null,
    val storyType: String = "image", // "image", "text"
    val textContent: String = "",
    val backgroundColor: String = "#1877F2"
)

class CreateStoryViewModel : ViewModel() {
    private val repo = SocialRepository()
    private val storage = FirebaseStorage.getInstance()
    
    var state by mutableStateOf(CreateStoryState())
        private set

    fun onImageSelected(uri: Uri) {
        state = state.copy(selectedUri = uri, storyType = "image")
    }

    fun onTextTypeSelected() {
        state = state.copy(storyType = "text", selectedUri = null)
    }

    fun updateText(text: String) {
        state = state.copy(textContent = text)
    }

    fun updateColor(color: String) {
        state = state.copy(backgroundColor = color)
    }

    fun createStory() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val uid = repo.currentUid ?: throw Exception("Not logged in")
                var finalImageUrl = ""
                
                if (state.storyType == "image" && state.selectedUri != null) {
                    val fileName = "stories/${UUID.randomUUID()}.jpg"
                    val storageRef = storage.reference.child(fileName)
                    storageRef.putFile(state.selectedUri!!).await()
                    finalImageUrl = storageRef.downloadUrl.await().toString()
                }

                val calendar = Calendar.getInstance()
                val createdAt = Timestamp.now()
                calendar.time = createdAt.toDate()
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val expiresAt = Timestamp(calendar.time)

                val story = Story(
                    userId = uid,
                    imageUrl = finalImageUrl,
                    text = state.textContent,
                    backgroundColor = state.backgroundColor,
                    type = state.storyType,
                    createdAt = createdAt,
                    expiresAt = expiresAt
                )

                // We need a way to save this Story. I'll update SocialRepository to accept a Story object.
                // For now, I'll assume we update createStory in Repo.
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
