package com.socialapp.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToProfile: (String?) -> Unit,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToComments: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: (String) -> Unit,
    onNavigateToAdmin: () -> Unit,
    feedViewModel: FeedViewModel = viewModel()
) {
    val state = feedViewModel.state
    var searchQuery by remember { mutableStateOf("") }
    var barsVisible by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(152.dp) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10f) barsVisible = false else if (available.y > 10f) barsVisible = true
                return Offset.Zero
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SocialApp") },
                actions = {
                    if (state.currentUser?.role == "admin") {
                        IconButton(onClick = onNavigateToAdmin) { Icon(Icons.Default.AdminPanelSettings, "Admin", tint = MaterialTheme.colorScheme.primary) }
                    }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings") }
                    TextButton(onClick = onLogout) { Text("Logout") }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB)))).padding(padding)) {
            Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
                if (state.isLoading && state.posts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = topBarHeight, bottom = 8.dp)) {
                        items(state.posts, key = { it.id }) { post ->
                            PostItem(
                                post = post, user = state.userMap[post.user_id], isLiked = post.id in state.likedIds, isSaved = post.id in state.savedIds, isFollowing = post.user_id in state.followingIds, isOwnPost = post.user_id == feedViewModel.currentUid,
                                onLike = { feedViewModel.toggleLike(post.id) }, onComment = { onNavigateToComments(post.id) }, onUserClick = { onNavigateToProfile(it) }, onSave = { feedViewModel.toggleSave(post.id) }, onFollow = { feedViewModel.toggleFollow(post.user_id) }
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(visible = barsVisible, enter = slideInVertically(initialOffsetY = { -it }), exit = slideOutVertically(targetOffsetY = { -it }), modifier = Modifier.align(Alignment.TopCenter)) {
                Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFE3F2FD)).onSizeChanged { topBarHeight = with(density) { it.height.toDp() } }) {
                    NavigationBar {
                        NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = true, onClick = { feedViewModel.loadFeed() })
                        NavigationBarItem(icon = { Icon(Icons.Default.Explore, null) }, label = { Text("Explore") }, selected = false, onClick = onNavigateToExplore)
                        NavigationBarItem(icon = { Icon(Icons.Default.Add, null) }, label = { Text("Post") }, selected = false, onClick = onNavigateToCreatePost)
                        NavigationBarItem(icon = { Icon(Icons.Default.Chat, null) }, label = { Text("Chat") }, selected = false, onClick = onNavigateToChat)
                        NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile") }, selected = false, onClick = { onNavigateToProfile(null) })
                    }
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(24.dp), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onNavigateToSearch(searchQuery) }))
                }
            }
        }
    }
}