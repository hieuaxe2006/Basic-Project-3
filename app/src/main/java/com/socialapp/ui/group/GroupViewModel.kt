package com.socialapp.ui.group

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.local.LocalCacheManager
import com.socialapp.data.model.Group
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch

data class GroupState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class GroupViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SocialRepository()
    private val cacheManager = LocalCacheManager(application)

    var state by mutableStateOf(GroupState())
        private set

    init {
        loadGroups()
    }

    fun loadGroups() {
        val cached = cacheManager.loadGroupsCache()
        if (cached != null) {
            state = state.copy(groups = cached)
        } else {
            state = state.copy(isLoading = true, error = null)
        }

        viewModelScope.launch {
            repo.getGroups().onSuccess { freshGroups ->
                state = state.copy(isLoading = false, groups = freshGroups, error = null)
                cacheManager.saveGroupsCache(freshGroups)
            }.onFailure {
                state = state.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun toggleJoinGroup(groupId: String) {
        viewModelScope.launch {
            val uid = repo.currentUid ?: return@launch
            val group = state.groups.find { it.id == groupId } ?: return@launch
            val isMember = group.memberIds.contains(uid)

            val result = if (isMember) {
                repo.leaveGroup(groupId)
            } else {
                repo.joinGroup(groupId)
            }

            result.onSuccess {
                val updatedGroups = state.groups.map { g ->
                    if (g.id == groupId) {
                        val newMembers = g.memberIds.toMutableList()
                        if (isMember) {
                            newMembers.remove(uid)
                            g.copy(
                                memberIds = newMembers,
                                memberCount = (g.memberCount - 1).coerceAtLeast(0)
                            )
                        } else {
                            newMembers.add(uid)
                            g.copy(
                                memberIds = newMembers,
                                memberCount = g.memberCount + 1
                            )
                        }
                    } else g
                }
                state = state.copy(groups = updatedGroups)
                cacheManager.saveGroupsCache(updatedGroups)
            }
        }
    }
}
