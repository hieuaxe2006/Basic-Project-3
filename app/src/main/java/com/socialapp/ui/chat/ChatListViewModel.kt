package com.socialapp.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.model.FriendRequest
import com.socialapp.data.model.User
import com.socialapp.data.repository.ChatPartner
import com.socialapp.data.repository.ChatRepository
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch

data class ChatListState(
    val partners: List<ChatPartner> = emptyList(),
    val searchResults: List<User> = emptyList(),
    val pendingRequests: List<Pair<FriendRequest, User>> = emptyList(),
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null
)

class ChatListViewModel : ViewModel() {
    private val repo = ChatRepository()
    private val socialRepo = SocialRepository()

    var state by mutableStateOf(ChatListState())
        private set

    init { 
        loadChatPartners()
        loadPendingRequests()
        loadCurrentUser()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            repo.getCurrentUser()?.let {
                state = state.copy(currentUser = it)
            }
        }
    }

    fun loadChatPartners() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            repo.getChatPartners()
                .onSuccess { state = state.copy(isLoading = false, partners = it) }
                .onFailure { state = state.copy(isLoading = false, error = it.message) }
        }
    }

    fun loadPendingRequests() {
        viewModelScope.launch {
            socialRepo.getPendingRequests()
                .onSuccess { state = state.copy(pendingRequests = it) }
        }
    }

    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            socialRepo.acceptFriendRequest(requestId).onSuccess {
                loadPendingRequests()
                loadChatPartners()
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            state = state.copy(searchResults = emptyList(), isSearching = false)
            return
        }
        viewModelScope.launch {
            state = state.copy(isSearching = true)
            repo.searchUsers(query)
                .onSuccess { state = state.copy(isSearching = false, searchResults = it) }
                .onFailure { state = state.copy(isSearching = false) }
        }
    }

    fun updateNote(note: String) {
        viewModelScope.launch {
            repo.updateNote(note).onSuccess {
                loadCurrentUser()
            }
        }
    }

    fun clearSearch() {
        state = state.copy(searchResults = emptyList())
    }
}
