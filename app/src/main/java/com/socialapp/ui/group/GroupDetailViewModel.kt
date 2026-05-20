package com.socialapp.ui.group

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.local.LocalCacheManager
import com.socialapp.data.model.Group
import com.socialapp.data.model.Post
import com.socialapp.data.model.User
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch

data class GroupDetailState(
    val group: Group? = null,
    val posts: List<Post> = emptyList(),
    val userMap: Map<String, User> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPosting: Boolean = false
)

class GroupDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SocialRepository()
    private val cacheManager = LocalCacheManager(application)

    var state by mutableStateOf(GroupDetailState())
        private set

    fun loadGroupDetail(groupId: String) {
        val cachedGroups = cacheManager.loadGroupsCache()
        val cachedGroup = cachedGroups?.find { it.id == groupId }
        if (cachedGroup != null) {
            state = state.copy(group = cachedGroup)
        } else {
            state = state.copy(isLoading = true, error = null)
        }

        viewModelScope.launch {
            repo.getGroupDetail(groupId).onSuccess { groupDetail ->
                state = state.copy(group = groupDetail, error = null)
                fetchPosts(groupId)
            }.onFailure {
                state = state.copy(isLoading = false, error = it.message)
            }
        }
    }

    private fun fetchPosts(groupId: String) {
        viewModelScope.launch {
            repo.getGroupPosts(groupId).onSuccess { groupPosts ->
                val userIds = groupPosts.map { it.user_id }.distinct()
                val userMap = state.userMap.toMutableMap()
                userIds.forEach { uid ->
                    if (!userMap.containsKey(uid)) {
                        repo.getUser(uid)?.let { userMap[uid] = it }
                    }
                }
                state = state.copy(isLoading = false, posts = groupPosts, userMap = userMap)
            }.onFailure {
                state = state.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun toggleJoinGroup() {
        val group = state.group ?: return
        val groupId = group.id
        viewModelScope.launch {
            val uid = repo.currentUid ?: return@launch
            val isMember = group.memberIds.contains(uid)

            val result = if (isMember) {
                repo.leaveGroup(groupId)
            } else {
                repo.joinGroup(groupId)
            }

            result.onSuccess {
                val newMembers = group.memberIds.toMutableList()
                val updatedGroup = if (isMember) {
                    newMembers.remove(uid)
                    group.copy(memberIds = newMembers, memberCount = (group.memberCount - 1).coerceAtLeast(0))
                } else {
                    newMembers.add(uid)
                    group.copy(memberIds = newMembers, memberCount = group.memberCount + 1)
                }
                state = state.copy(group = updatedGroup)
                
                val cachedGroups = cacheManager.loadGroupsCache()?.toMutableList()
                if (cachedGroups != null) {
                    val idx = cachedGroups.indexOfFirst { it.id == groupId }
                    if (idx != -1) {
                        cachedGroups[idx] = updatedGroup
                        cacheManager.saveGroupsCache(cachedGroups)
                    }
                }
            }
        }
    }

    fun createPost(content: String) {
        val group = state.group ?: return
        if (content.isBlank()) return

        state = state.copy(isPosting = true)
        viewModelScope.launch {
            repo.createGroupPost(group.id, content).onSuccess {
                repo.getGroupDetail(group.id).onSuccess { freshGroup ->
                    state = state.copy(group = freshGroup)
                }
                fetchPosts(group.id)
                state = state.copy(isPosting = false)
            }.onFailure {
                state = state.copy(isPosting = false, error = it.message)
            }
        }
    }
}
