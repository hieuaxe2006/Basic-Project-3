package com.socialapp.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            AnimatedVisibility(visible = barsVisible, enter = slideInVertically(initialOffsetY = { -it }), exit = slideOutVertically(targetOffsetY = { -it })) {
                Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "SocialApp", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = onNavigateToSettings, modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = onLogout) { Text("Logout") }
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search...") },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { if (searchQuery.isNotBlank()) onNavigateToSearch(searchQuery) })
                        )
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = barsVisible, enter = slideInVertically(initialOffsetY = { it }), exit = slideOutVertically(targetOffsetY = { it })) {
                NavigationBar {
                    NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") }, selected = true, onClick = { feedViewModel.loadFeed() })
                    NavigationBarItem(icon = { Icon(Icons.Default.Explore, null) }, label = { Text("Explore") }, selected = false, onClick = onNavigateToExplore)
                    NavigationBarItem(icon = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }, label = { Text("Post") }, selected = false, onClick = onNavigateToCreatePost)
                    NavigationBarItem(icon = { Icon(Icons.Default.Chat, null) }, label = { Text("Chat") }, selected = false, onClick = onNavigateToChat)
                    NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile") }, selected = false, onClick = { onNavigateToProfile(null) })
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).nestedScroll(nestedScrollConnection)) {
            when {
                state.isLoading && state.posts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                state.posts.isEmpty() -> {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("📝", fontSize = 48.sp)
                        Text("No posts yet", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = onNavigateToCreatePost) { Text("Create Post") }
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.posts, key = { it.id }) { post ->
                            PostItem(
                                post = post,
                                user = state.userMap[post.user_id],
                                isLiked = post.id in state.likedIds,
                                isSaved = post.id in state.savedIds,
                                isFollowing = post.user_id in state.followingIds,
                                isOwnPost = post.user_id == feedViewModel.currentUid,
                                onLike = { feedViewModel.toggleLike(post.id) },
                                onComment = { onNavigateToComments(post.id) },
                                onUserClick = { onNavigateToProfile(it) },
                                onSave = { feedViewModel.toggleSave(post.id) },
                                onFollow = { feedViewModel.toggleFollow(post.user_id) }
                            )
                        }
                    }
                }
            }
        }
    }
}