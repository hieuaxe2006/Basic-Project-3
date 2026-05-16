package com.socialapp.ui.post

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.model.Story
import com.socialapp.data.model.User
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch

class StoryViewViewModel : ViewModel() {
    private val repo = SocialRepository()
    
    var story by mutableStateOf<Story?>(null)
        private set
    
    var user by mutableStateOf<User?>(null)
        private set

    fun loadStory(storyId: String) {
        viewModelScope.launch {
            val s = repo.getStory(storyId)
            story = s
            if (s != null) {
                user = repo.getUser(s.userId)
            }
        }
    }
}
