package com.socialapp.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.socialapp.data.local.CachedFeedData
import com.socialapp.data.local.LocalCacheManager
import com.socialapp.data.model.Post
import com.socialapp.data.model.Story
import com.socialapp.data.model.User
import com.socialapp.data.repository.ChatRepository
import com.socialapp.data.repository.SocialRepository
import com.socialapp.data.repository.UserRepository
import kotlinx.coroutines.flow.collect
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
    val unreadNotificationCount: Int = 0,
    val isPreloading: Boolean = false,
    val isEndReached: Boolean = false
)

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SocialRepository()
    private val userRepo = UserRepository()
    private val chatRepo = ChatRepository()
    private val db = FirebaseFirestore.getInstance()
    private var messageListener: ListenerRegistration? = null
    private var notificationListener: ListenerRegistration? = null
    private val cacheManager = LocalCacheManager(application)

    private var lastVisibleTimestamp: Timestamp? = null
    private var preloadedPosts: List<Post>? = null
    private var preloadedUserMap: Map<String, User> = emptyMap()
    private var preloadedLikedIds: Set<String> = emptySet()
    private var preloadedLastTimestamp: Timestamp? = null
    private var isPreloadedEndReached: Boolean = false

    var state by mutableStateOf(FeedState())
        private set
    val currentUid get() = repo.currentUid

    init {
        loadCachedData()
        loadFeed(isRefresh = true)
        loadStories()
        observeCurrentUserRealtime() // CẬP NHẬT: Lắng nghe realtime
        observeSavedPosts()
        observeFollowing()
        observeUnreadMessages()
        observeUnreadNotifications()
    }

    // Lắng nghe dữ liệu User thời gian thực để cập nhật Premium ngay lập tức
    private fun observeCurrentUserRealtime() {
        val uid = currentUid ?: return
        viewModelScope.launch {
            userRepo.getUserSnapshot(uid).collect { user ->
                val updatedUserMap = state.userMap.toMutableMap()
                updatedUserMap[uid] = user
                state = state.copy(currentUser = user, userMap = updatedUserMap)
            }
        }
    }

    private fun loadCachedData() {
        val cached = cacheManager.loadFeedCache()
        if (cached != null) {
            state = state.copy(
                posts = cached.posts,
                stories = cached.stories,
                userMap = state.userMap + cached.userMap,
                likedIds = cached.likedIds,
                savedIds = cached.savedIds
            )
            lastVisibleTimestamp = cached.posts.lastOrNull()?.created_at
        }
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

    fun loadFeed(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                lastVisibleTimestamp = null
                state = state.copy(isEndReached = false)
                preloadedPosts = null
                preloadedUserMap = emptyMap()
                preloadedLikedIds = emptySet()
                preloadedLastTimestamp = null
                isPreloadedEndReached = false
            }

            if (isRefresh || state.posts.isEmpty()) {
                state = state.copy(isLoading = true, error = null)
            } else {
                state = state.copy(error = null)
            }

            repo.getFeed(limit = 10, lastVisibleTimestamp = null).onSuccess { posts ->
                val userIds = posts.map { it.user_id }.distinct()
                val userMap = state.userMap.toMutableMap()
                userIds.forEach { uid ->
                    if (!userMap.containsKey(uid)) {
                        repo.getUser(uid)?.let { userMap[uid] = it }
                    }
                }
                val likedIds = repo.getLikedPostIds(posts.map { it.id })

                lastVisibleTimestamp = posts.lastOrNull()?.created_at
                val isEnd = posts.size < 10

                state = state.copy(
                    isLoading = false,
                    posts = posts,
                    userMap = userMap,
                    likedIds = likedIds,
                    isEndReached = isEnd
                )

                cacheManager.saveFeedCache(
                    CachedFeedData(posts = posts, stories = state.stories, userMap = userMap, likedIds = likedIds, savedIds = state.savedIds)
                )

                if (!isEnd) { preloadNextPage() }
            }.onFailure {
                state = state.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun preloadNextPage() {
        if (state.isPreloading || state.isEndReached || preloadedPosts != null) return
        val timestamp = lastVisibleTimestamp ?: return
        viewModelScope.launch {
            state = state.copy(isPreloading = true)
            repo.getFeed(limit = 10, lastVisibleTimestamp = timestamp).onSuccess { posts ->
                if (posts.isNotEmpty()) {
                    val userIds = posts.map { it.user_id }.distinct()
                    val userMap = mutableMapOf<String, User>()
                    userIds.forEach { uid -> repo.getUser(uid)?.let { userMap[uid] = it } }
                    val likedIds = repo.getLikedPostIds(posts.map { it.id })
                    preloadedPosts = posts
                    preloadedUserMap = userMap
                    preloadedLikedIds = likedIds
                    preloadedLastTimestamp = posts.lastOrNull()?.created_at
                    isPreloadedEndReached = posts.size < 10
                } else {
                    isPreloadedEndReached = true
                }
                state = state.copy(isPreloading = false)
            }.onFailure {
                state = state.copy(isPreloading = false)
            }
        }
    }

    fun loadNextPage() {
        if (state.isLoading || state.isEndReached) return
        val preloaded = preloadedPosts
        if (preloaded != null) {
            val updatedPosts = state.posts + preloaded
            val updatedUserMap = state.userMap + preloadedUserMap
            val updatedLikedIds = state.likedIds + preloadedLikedIds
            lastVisibleTimestamp = preloadedLastTimestamp
            val isEnd = isPreloadedEndReached
            state = state.copy(posts = updatedPosts, userMap = updatedUserMap, likedIds = updatedLikedIds, isEndReached = isEnd)
            preloadedPosts = null
            preloadedUserMap = emptyMap()
            preloadedLikedIds = emptySet()
            preloadedLastTimestamp = null
            isPreloadedEndReached = false
            if (!isEnd) { preloadNextPage() }
        } else {
            val timestamp = lastVisibleTimestamp ?: return
            viewModelScope.launch {
                state = state.copy(isLoading = true)
                repo.getFeed(limit = 10, lastVisibleTimestamp = timestamp).onSuccess { posts ->
                    val userIds = posts.map { it.user_id }.distinct()
                    val userMap = state.userMap.toMutableMap()
                    userIds.forEach { uid -> if (!userMap.containsKey(uid)) { repo.getUser(uid)?.let { userMap[uid] = it } } }
                    val likedIds = repo.getLikedPostIds(posts.map { it.id })
                    lastVisibleTimestamp = posts.lastOrNull()?.created_at
                    val isEnd = posts.size < 10
                    state = state.copy(isLoading = false, posts = state.posts + posts, userMap = userMap, likedIds = state.likedIds + likedIds, isEndReached = isEnd)
                    if (!isEnd) { preloadNextPage() }
                }.onFailure { state = state.copy(isLoading = false, error = it.message) }
            }
        }
    }

    fun loadStories() {
        viewModelScope.launch {
            repo.getStories().onSuccess { stories ->
                val userIds = stories.map { it.userId }.distinct()
                val userMap = state.userMap.toMutableMap()
                userIds.forEach { uid -> if (!userMap.containsKey(uid)) { repo.getUser(uid)?.let { userMap[uid] = it } } }
                state = state.copy(stories = stories, userMap = userMap)
                cacheManager.saveFeedCache(CachedFeedData(posts = state.posts, stories = stories, userMap = userMap, likedIds = state.likedIds, savedIds = state.savedIds))
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
            repo.getFriends().onSuccess { state = state.copy(isListLoading = false, friendsList = it) }.onFailure { state = state.copy(isListLoading = false, error = it.message) }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) { state = state.copy(searchResults = emptyList()); return }
        viewModelScope.launch {
            state = state.copy(isSearching = true)
            repo.searchUsers(query).onSuccess { state = state.copy(isSearching = false, searchResults = it) }.onFailure { state = state.copy(isSearching = false) }
        }
    }

    fun sharePost(post: Post, friend: User) {
        viewModelScope.launch {
            state = state.copy(isSharing = true, shareSuccess = null)
            // Ghép link danh sách ảnh
            val imgs = post.image_urls.joinToString("\n")
            val shareContent = "Chia sẻ bài viết:\n${post.content}\n$imgs".trim()
            chatRepo.sendMessage(friend.id, shareContent).onSuccess { state = state.copy(isSharing = false, shareSuccess = "Đã gửi đến ${friend.username}") }.onFailure { state = state.copy(isSharing = false, error = it.message) }
        }
    }

    fun clearShareState() { state = state.copy(shareSuccess = null) }
    fun toggleSave(postId: String) = viewModelScope.launch { repo.toggleSavePost(postId) }
    fun toggleFollow(targetUid: String) = viewModelScope.launch { repo.toggleFollow(targetUid) }
    private fun observeSavedPosts() = viewModelScope.launch { repo.getSavedPostIdsFlow().collect { state = state.copy(savedIds = it) } }
    private fun observeFollowing() = viewModelScope.launch { repo.getFollowingIdsFlow().collect { state = state.copy(followingIds = it) } }

    fun updatePostVisibility(postId: String, isPrivate: Boolean) {
        viewModelScope.launch { repo.updatePostVisibility(postId, isPrivate).onSuccess { val updated = state.posts.map { if (it.id == postId) it.copy(is_private = isPrivate) else it }; state = state.copy(posts = updated) } }
    }

    fun updatePostCommentsDisabled(postId: String, commentsDisabled: Boolean) {
        viewModelScope.launch { repo.updatePostCommentsDisabled(postId, commentsDisabled).onSuccess { val updated = state.posts.map { if (it.id == postId) it.copy(comments_disabled = commentsDisabled) else it }; state = state.copy(posts = updated) } }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch { repo.deletePost(postId).onSuccess { val updated = state.posts.filter { it.id != postId }; state = state.copy(posts = updated) } }
    }

    override fun onCleared() {
        messageListener?.remove()
        notificationListener?.remove()
        super.onCleared()
    }
}