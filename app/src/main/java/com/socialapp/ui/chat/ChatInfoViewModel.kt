package com.socialapp.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch

class ChatInfoViewModel : ViewModel() {
    private val repo = SocialRepository()
    
    var isMuted by mutableStateOf(false)
    var isBlocked by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    fun blockUser(targetUid: String) {
        viewModelScope.launch {
            repo.blockUser(targetUid).onSuccess {
                isBlocked = true
                message = "Đã chặn người dùng này"
            }.onFailure {
                message = "Lỗi: ${it.message}"
            }
        }
    }

    fun muteUser(targetUid: String) {
        viewModelScope.launch {
            repo.muteUser(targetUid).onSuccess {
                isMuted = true
                message = "Đã tắt thông báo"
            }.onFailure {
                message = "Lỗi: ${it.message}"
            }
        }
    }

    fun reportUser(targetUid: String, reason: String) {
        viewModelScope.launch {
            repo.reportUser(targetUid, reason).onSuccess {
                message = "Cảm ơn bạn đã báo cáo. Chúng tôi sẽ xem xét sớm nhất."
            }.onFailure {
                message = "Lỗi: ${it.message}"
            }
        }
    }
    
    fun clearMessage() {
        message = null
    }
}
