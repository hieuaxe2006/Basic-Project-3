package com.socialapp.ui.explore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch

data class ExploreState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ExploreViewModel : ViewModel() {
    private val repo = SocialRepository()

    var state by mutableStateOf(ExploreState())
        private set

    private val fixedDomains = listOf("Code", "Life", "Study", "Animal", "Food", "Exercise", "Music", "Travel")

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            repo.getDomainCounts()
                .onSuccess { map ->
                    val categories = fixedDomains.map { name ->
                        Category(name, map.getOrDefault(name, 0))
                    }
                    state = state.copy(isLoading = false, categories = categories)
                }
                .onFailure { 
                    state = state.copy(isLoading = false, error = it.message) 
                }
        }
    }
}
