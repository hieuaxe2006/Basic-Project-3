package com.socialapp.ui.chat

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.socialapp.data.model.Message
import com.socialapp.data.remote.ImgBBApi
import com.socialapp.data.repository.ChatRepository
import kotlinx.coroutines.launch

data class ChatState(
    val messages: List<Message> = emptyList(),
    val isSending: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null
)

class ChatViewModel : ViewModel() {
    private val repo = ChatRepository()
    private var listener: ListenerRegistration? = null

    var state by mutableStateOf(ChatState())
        private set

    val currentUid get() = repo.currentUid

    fun startListening(otherUid: String) {
        // NGAY KHI MỞ CHAT: Đánh dấu tất cả tin nhắn từ người này gửi cho mình là đã xem
        viewModelScope.launch {
            repo.markAsRead(otherUid)
        }

        listener?.remove()
        listener = repo.listenMessages(otherUid) { messages ->
            state = state.copy(messages = messages)

            // KIỂM TRA REALTIME: Nếu đang mở màn hình chat mà đối phương gửi tin nhắn tới,
            // tự động cập nhật tin nhắn đó thành "đã xem" ngay lập tức.
            val hasUnreadFromPartner = messages.any { it.sender_id == otherUid && !it.seen }
            if (hasUnreadFromPartner) {
                viewModelScope.launch {
                    repo.markAsRead(otherUid)
                }
            }
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

    fun sendImage(receiverId: String, uri: Uri, context: Context) {
        viewModelScope.launch {
            state = state.copy(isUploading = true)
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                val base64 = bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                    ?: throw Exception("Failed to read image")
                
                ImgBBApi.uploadImage(base64).onSuccess { url ->
                    repo.sendMessage(receiverId, "Sent an image:\n$url")
                        .onFailure { state = state.copy(error = it.message) }
                }.onFailure {
                    state = state.copy(error = it.message)
                }
            } catch (e: Exception) {
                state = state.copy(error = e.message)
            } finally {
                state = state.copy(isUploading = false)
            }
        }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}