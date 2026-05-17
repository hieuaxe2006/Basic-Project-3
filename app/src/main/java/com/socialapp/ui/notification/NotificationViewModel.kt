package com.socialapp.ui.notification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.model.Notification
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch

data class NotificationState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = true, // Mặc định là true để chờ dữ liệu đầu tiên
    val hasLoadedOnce: Boolean = false
)

class NotificationViewModel : ViewModel() {
    private val repo = SocialRepository()
    
    var state by mutableStateOf(NotificationState())
        private set

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            // Không set isLoading = true ở đây nếu đã có dữ liệu để tránh flicker
            repo.getNotificationsFlow().collect { newList ->
                state = state.copy(
                    notifications = newList, 
                    isLoading = false,
                    hasLoadedOnce = true
                )
            }
        }
    }

    fun markAsSeen(notificationId: String) {
        viewModelScope.launch {
            repo.markNotificationAsSeen(notificationId)
        }
    }

    fun markAllAsSeen() {
        viewModelScope.launch {
            repo.markAllNotificationsAsSeen()
        }
    }
}
