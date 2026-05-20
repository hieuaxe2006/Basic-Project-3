package com.socialapp.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.socialapp.data.model.Post
import com.socialapp.data.model.Story
import com.socialapp.data.model.User
import com.socialapp.data.repository.ChatRepository
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch

data class FeedState(
    val posts: List<Post> = emptyList(),
    val stories: List<Story> = emptyList(),
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
    val isSearching: Boolean = false,
    val unreadChatCount: Int = 0,
    val unreadNotificationCount: Int = 0
)

class FeedViewModel : ViewModel() {
    private val repo = SocialRepository()
    private val chatRepo = ChatRepository()
    private val db = FirebaseFirestore.getInstance()
    private var messageListener: ListenerRegistration? = null
    private var notificationListener: ListenerRegistration? = null

    var state by mutableStateOf(FeedState())
        private set
    val currentUid get() = repo.currentUid

    init {
        loadFeed()
        loadStories()
        loadCurrentUser()
        observeSavedPosts()
        observeFollowing()
        observeUnreadMessages()
        observeUnreadNotifications()
    }

    private fun observeUnreadMessages() {
        val uid = currentUid ?: return
        messageListener?.remove()
        messageListener = db.collection("messages")
            .whereEqualTo("receiver_id", uid)
            .whereEqualTo("seen", false)
            .addSnapshotListener { snapshot, _ ->
                val count = snapshot?.size() ?: 0
                state = state.copy(unreadChatCount = count)
            }
    }

    private fun observeUnreadNotifications() {
        val uid = currentUid ?: return
        notificationListener?.remove()
        notificationListener = db.collection("notifications")
            .whereEqualTo("receiverId", uid)
            .whereEqualTo("isSeen", false)
            .addSnapshotListener { snapshot, _ ->
                val count = snapshot?.size() ?: 0
                state = state.copy(unreadNotificationCount = count)
            }
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
                val userMap = state.userMap.toMutableMap()
                userIds.forEach { uid -> 
                    if (!userMap.containsKey(uid)) {
                        repo.getUser(uid)?.let { userMap[uid] = it }
                    }
                }
                val likedIds = repo.getLikedPostIds(posts.map { it.id })
                state = state.copy(isLoading = false, posts = posts, userMap = userMap, likedIds = likedIds)
            }.onFailure { state = state.copy(isLoading = false, error = it.message) }
        }
    }

    fun loadStories() {
        viewModelScope.launch {
            repo.getStories().onSuccess { stories ->
                val userIds = stories.map { it.userId }.distinct()
                val userMap = state.userMap.toMutableMap()
                userIds.forEach { uid ->
                    if (!userMap.containsKey(uid)) {
                        repo.getUser(uid)?.let { userMap[uid] = it }
                    }
                }
                state = state.copy(stories = stories, userMap = userMap)
            }
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

    fun updatePostVisibility(postId: String, isPrivate: Boolean) {
        viewModelScope.launch {
            repo.updatePostVisibility(postId, isPrivate).onSuccess {
                val updated = state.posts.map { if (it.id == postId) it.copy(is_private = isPrivate) else it }
                state = state.copy(posts = updated)
            }
        }
    }

    fun updatePostCommentsDisabled(postId: String, commentsDisabled: Boolean) {
        viewModelScope.launch {
            repo.updatePostCommentsDisabled(postId, commentsDisabled).onSuccess {
                val updated = state.posts.map { if (it.id == postId) it.copy(comments_disabled = commentsDisabled) else it }
                state = state.copy(posts = updated)
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repo.deletePost(postId).onSuccess {
                val updated = state.posts.filter { it.id != postId }
                state = state.copy(posts = updated)
            }
        }
    }

    override fun onCleared() {
        messageListener?.remove()
        notificationListener?.remove()
        super.onCleared()
    }
}
