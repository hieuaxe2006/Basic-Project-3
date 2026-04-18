package com.socialapp.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.model.User
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch

data class SearchState(
    val results: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SearchViewModel : ViewModel() {
    private val repo = SocialRepository()

    var state by mutableStateOf(SearchState())
        private set

    fun performSearch(query: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            repo.searchUsers(query)
                .onSuccess { users ->
                    state = state.copy(isLoading = false, results = users)
                }
                .onFailure {
                    state = state.copy(isLoading = false, error = it.message)
                }
        }
    }
}
