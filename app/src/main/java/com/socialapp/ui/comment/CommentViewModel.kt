package com.socialapp.ui.comment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.model.Comment
import com.socialapp.data.model.Post
import com.socialapp.data.model.User
import com.socialapp.data.repository.SocialRepository
import kotlinx.coroutines.launch

data class CommentState(
    val post: Post? = null,
    val postUser: User? = null,
    val comments: List<Comment> = emptyList(),
    val userMap: Map<String, User> = emptyMap(),
    val likedCommentIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

class CommentViewModel : ViewModel() {
    private val repo = SocialRepository()

    var state by mutableStateOf(CommentState())
        private set

    fun loadComments(postId: String) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            
            val postResult = repo.getPost(postId).getOrNull()
            val postUser = postResult?.user_id?.let { repo.getUser(it) }
            
            repo.getComments(postId)
                .onSuccess { comments ->
                    val userIds = (comments.map { it.user_id } + (postResult?.user_id ?: "")).distinct().filter { it.isNotBlank() }
                    val userMap = mutableMapOf<String, User>()
                    userIds.forEach { uid ->
                        repo.getUser(uid)?.let { userMap[uid] = it }
                    }
                    val likedIds = repo.getLikedCommentIds(comments.map { it.id })
                    state = state.copy(
                        isLoading = false,
                        comments = comments,
                        userMap = userMap,
                        post = postResult,
                        postUser = postUser ?: userMap[postResult?.user_id],
                        likedCommentIds = likedIds
                    )
                }
                .onFailure { 
                    state = state.copy(
                        isLoading = false, 
                        post = postResult, 
                        postUser = postUser,
                        error = it.message 
                    ) 
                }
        }
    }

    fun addComment(postId: String, content: String, parentId: String = "") {
        if (content.isBlank()) return
        viewModelScope.launch {
            state = state.copy(isSending = true)
            repo.addComment(postId, content, parentId)
                .onSuccess { comment ->
                    val uid = comment.user_id
                    val userMap = state.userMap.toMutableMap()
                    if (uid !in userMap) {
                        repo.getUser(uid)?.let { userMap[uid] = it }
                    }
                    state = state.copy(
                        isSending = false,
                        comments = listOf(comment) + state.comments,
                        userMap = userMap
                    )
                }
                .onFailure { state = state.copy(isSending = false, error = it.message) }
        }
    }

    fun toggleCommentLike(commentId: String) {
        viewModelScope.launch {
            repo.toggleCommentLike(commentId)
                .onSuccess { liked ->
                    val updatedComments = state.comments.map { c ->
                        if (c.id == commentId) c.copy(like_count = c.like_count + if (liked) 1 else -1)
                        else c
                    }
                    val updatedLikedIds = if (liked) state.likedCommentIds + commentId else state.likedCommentIds - commentId
                    state = state.copy(comments = updatedComments, likedCommentIds = updatedLikedIds)
                }
        }
    }
}
