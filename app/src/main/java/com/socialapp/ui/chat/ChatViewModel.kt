package com.socialapp.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.socialapp.data.model.Message
import com.socialapp.data.repository.ChatRepository
import kotlinx.coroutines.launch

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isSending: Boolean = false,
    val error: String? = null
)

class ChatViewModel : ViewModel() {
    private val repo = ChatRepository()
    private var listener: ListenerRegistration? = null

    var state by mutableStateOf(ChatState())
        private set

    val currentUid get() = repo.currentUid

    fun startListening(otherUid: String) {
        listener?.remove()
        listener = repo.listenMessages(otherUid) { messages ->
            state = state.copy(messages = messages)
        }
    }

    fun sendMessage(receiverId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            state = state.copy(isSending = true)
            repo.sendMessage(receiverId, content)
                .onFailure { state = state.copy(error = it.message) }
            state = state.copy(isSending = false)
        }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}
