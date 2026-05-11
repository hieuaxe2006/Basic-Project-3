package com.socialapp.ui.explore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.socialapp.ui.home.PostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFeedScreen(
    tag: String,
    onBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToComments: (String) -> Unit,
    vm: CategoryFeedViewModel = viewModel()
) {
    val state = vm.state

    LaunchedEffect(tag) {
        vm.loadPostsByTag(tag)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("#$tag", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.posts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Chưa có bài viết nào thuộc chủ đề này")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(state.posts, key = { it.id }) { post ->
                    PostItem(
                        post = post,
                        user = state.userMap[post.user_id],
                        isLiked = post.id in state.likedIds,
                        isSaved = post.id in state.savedIds,
                        isFollowing = post.user_id in state.followingIds,
                        isOwnPost = post.user_id == vm.currentUid,
                        onLike = { vm.toggleLike(post.id) },
                        onComment = { onNavigateToComments(post.id) },
                        onUserClick = { onNavigateToProfile(it) },
                        onSave = { vm.toggleSave(post.id) },
                        onFollow = { vm.toggleFollow(post.user_id) }
                    )
                }
            }
        }
    }
}