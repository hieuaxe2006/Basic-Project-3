package com.socialapp.ui.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.socialapp.data.model.Post
import com.socialapp.data.model.User

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
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var barsVisible by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0.dp) }
    
    var showShareSheet by remember { mutableStateOf(false) }
    var postToShare by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(state.shareSuccess) {
        state.shareSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            feedViewModel.clearShareState()
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10f) barsVisible = false else if (available.y > 10f) barsVisible = true
                return Offset.Zero
            }
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = barsVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.shadow(elevation = 8.dp)
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) },
                        label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                        selected = true,
                        onClick = { feedViewModel.loadFeed() },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Explore", modifier = Modifier.size(24.dp)) },
                        label = { Text("Explore", style = MaterialTheme.typography.labelSmall) },
                        selected = false,
                        onClick = onNavigateToExplore,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Add, contentDescription = "Create Post", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary) },
                        label = { Text("Post", style = MaterialTheme.typography.labelSmall) },
                        selected = false,
                        onClick = onNavigateToCreatePost,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat", modifier = Modifier.size(24.dp)) },
                        label = { Text("Chat", style = MaterialTheme.typography.labelSmall) },
                        selected = false,
                        onClick = onNavigateToChat,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(24.dp)) },
                        label = { Text("Profile", style = MaterialTheme.typography.labelSmall) },
                        selected = false,
                        onClick = { onNavigateToProfile(null) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(nestedScrollConnection)
        ) {
            when {
                state.isLoading && state.posts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                    }
                }

                state.error != null && state.posts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { feedViewModel.loadFeed() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Retry", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                state.posts.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📝", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("No posts yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text("Be the first to share something!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToCreatePost,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Create Post", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = topBarHeight + 4.dp, bottom = 4.dp)
                    ) {
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
                                onUserClick = { uid -> onNavigateToProfile(uid) },
                                onSave = { feedViewModel.toggleSave(post.id) },
                                onFollow = { feedViewModel.toggleFollow(post.user_id) },
                                onShare = {
                                    postToShare = post
                                    showShareSheet = true
                                    feedViewModel.loadFriends()
                                }
                            )
                        }
                    }
                }
            }

            // Top Bar Overlay
            AnimatedVisibility(
                visible = barsVisible,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    shadowElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.onSizeChanged { topBarHeight = with(density) { it.height.toDp() } }
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SocialApp",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.weight(1f))

                            if (state.currentUser?.role == "admin") {
                                IconButton(onClick = onNavigateToAdmin) {
                                    Icon(Icons.Default.AdminPanelSettings, "Admin", tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                            }

                            Spacer(Modifier.width(8.dp))

                            TextButton(
                                onClick = onLogout,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Text("Logout", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search posts, users...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { if (searchQuery.isNotBlank()) onNavigateToSearch(searchQuery) })
                        )

                        if (searchQuery.isNotBlank() && state.recommendedUsers.isNotEmpty()) {
                            Text(
                                "Recommended: " + state.recommendedUsers.take(3).joinToString(", ") { "@${it.username}" },
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showShareSheet) {
        ModalBottomSheet(onDismissRequest = { showShareSheet = false }) {
            ShareSheetContent(
                friends = state.friendsList,
                isLoading = state.isListLoading,
                isSharing = state.isSharing,
                onFriendClick = { friend ->
                    postToShare?.let { post ->
                        feedViewModel.sharePost(post, friend)
                    }
                    showShareSheet = false
                }
            )
        }
    }
}

@Composable
private fun ShareSheetContent(
    friends: List<User>,
    isLoading: Boolean,
    isSharing: Boolean,
    onFriendClick: (User) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f).padding(16.dp)) {
        Text("Gửi bài viết đến bạn bè", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (friends.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Chưa có bạn bè để chia sẻ") }
        } else {
            LazyColumn {
                items(friends) { friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!isSharing) onFriendClick(friend) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (friend.avatar.isNotBlank()) {
                            AsyncImage(
                                model = friend.avatar,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Box(contentAlignment = Alignment.Center) { Text(friend.username.take(1).uppercase()) }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(friend.username, modifier = Modifier.weight(1f))
                        if (isSharing) CircularProgressIndicator(Modifier.size(16.dp))
                        else Icon(Icons.AutoMirrored.Filled.Chat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
