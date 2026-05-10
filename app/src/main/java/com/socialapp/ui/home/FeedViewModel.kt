package com.socialapp.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.model.Post
import com.socialapp.data.model.User
import com.socialapp.data.repository.ChatRepository
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch

data class FeedState(
    val posts: List<Post> = emptyList(),
    val userMap: Map<String, User> = emptyMap(),
    val currentUser: User? = null,
    val recommendedUsers: List<User> = emptyList(),
    val likedIds: Set<String> = emptySet(),
    val savedIds: Set<String> = emptySet(),
    val followingIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val friendsList: List<User> = emptyList(),
    val isListLoading: Boolean = false,
    val isSharing: Boolean = false,
    val shareSuccess: String? = null,
    val searchResults: List<User> = emptyList(),
    val isSearching: Boolean = false
)

class FeedViewModel : ViewModel() {
    private val repo = SocialRepository()
    private val chatRepo = ChatRepository()
    var state by mutableStateOf(FeedState())
        private set
    val currentUid get() = repo.currentUid

    init {
        loadFeed()
        loadCurrentUser()
        observeSavedPosts()
        observeFollowing()
    }

    private fun loadCurrentUser() {
        val uid = currentUid ?: return
        viewModelScope.launch {
            val user = repo.getUser(uid)
            state = state.copy(currentUser = user)
        }
    }

    fun loadFeed() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            repo.getFeed().onSuccess { posts ->
                val userIds = posts.map { it.user_id }.distinct()
                val userMap = mutableMapOf<String, User>()
                userIds.forEach { uid -> repo.getUser(uid)?.let { userMap[uid] = it } }
                val likedIds = repo.getLikedPostIds(posts.map { it.id })
                state = state.copy(isLoading = false, posts = posts, userMap = userMap, likedIds = likedIds)
            }.onFailure { state = state.copy(isLoading = false, error = it.message) }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            repo.toggleLike(postId).onSuccess { liked ->
                val updatedPosts = state.posts.map { if (it.id == postId) it.copy(like_count = it.like_count + if (liked) 1 else -1) else it }
                val updatedLikedIds = if (liked) state.likedIds + postId else state.likedIds - postId
                state = state.copy(posts = updatedPosts, likedIds = updatedLikedIds)
            }
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            state = state.copy(isListLoading = true)
            repo.getFriends()
                .onSuccess { state = state.copy(isListLoading = false, friendsList = it) }
                .onFailure { state = state.copy(isListLoading = false, error = it.message) }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            state = state.copy(searchResults = emptyList())
            return
        }
        viewModelScope.launch {
            state = state.copy(isSearching = true)
            repo.searchUsers(query)
                .onSuccess { state = state.copy(isSearching = false, searchResults = it) }
                .onFailure { state = state.copy(isSearching = false) }
        }
    }

    fun sharePost(post: Post, friend: User) {
        viewModelScope.launch {
            state = state.copy(isSharing = true, shareSuccess = null)
            val shareContent = "Chia sẻ bài viết:\n${post.content}\n${post.image_url}".trim()
            chatRepo.sendMessage(friend.id, shareContent)
                .onSuccess {
                    state = state.copy(isSharing = false, shareSuccess = "Đã gửi đến ${friend.username}")
                }
                .onFailure {
                    state = state.copy(isSharing = false, error = it.message)
                }
        }
    }

    fun clearShareState() {
        state = state.copy(shareSuccess = null)
    }

    fun toggleSave(postId: String) = viewModelScope.launch { repo.toggleSavePost(postId) }
    fun toggleFollow(targetUid: String) = viewModelScope.launch { repo.toggleFollow(targetUid) }
    private fun observeSavedPosts() = viewModelScope.launch { repo.getSavedPostIdsFlow().collect { state = state.copy(savedIds = it) } }
    private fun observeFollowing() = viewModelScope.launch { repo.getFollowingIdsFlow().collect { state = state.copy(followingIds = it) } }
}