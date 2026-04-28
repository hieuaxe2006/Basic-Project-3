package com.socialapp.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.socialapp.ui.theme.*

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
        // ===== Top App Bar - Facebook style =====
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
            Surface(
                shadowElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    // Header row with title + action icons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App name - 20px bold per design system
                        Text(
                            text = "SocialApp",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.weight(1f))

                        // Settings icon button - subtle style per design system
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        // Logout button - subtle style
                        TextButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                "Logout",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // Search bar - border radius 10px, border #DADDE1, focus outline #1877F2
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search posts, users...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) onNavigateToSearch(searchQuery)
                            }
                        )
                    )

                    // Recommended users hint
                    if (searchQuery.isNotBlank() && state.recommendedUsers.isNotEmpty()) {
                        val recommendationText = "Recommended: " +
                                state.recommendedUsers.take(3).joinToString(", ") { "@${it.username}" }
                        Text(
                            recommendationText,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },

        // ===== Bottom Navigation Bar =====
        bottomBar = {
            AnimatedVisibility(
                visible = barsVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .shadow(elevation = 8.dp)
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = "Home",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text("Home", style = MaterialTheme.typography.labelSmall)
                        },
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
                        icon = {
                            Icon(
                                Icons.Default.Explore,
                                contentDescription = "Explore",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text("Explore", style = MaterialTheme.typography.labelSmall)
                        },
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
                        icon = {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Create Post",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        label = {
                            Text("Post", style = MaterialTheme.typography.labelSmall)
                        },
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
                        icon = {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = "Chat",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text("Chat", style = MaterialTheme.typography.labelSmall)
                        },
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
                        icon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text("Profile", style = MaterialTheme.typography.labelSmall)
                        },
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

        // Background color from design system: #F0F2F5
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
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                    OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search...") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(24.dp), singleLine = true, keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onNavigateToSearch(searchQuery) }))
                }

                state.error != null && state.posts.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                state.error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { feedViewModel.loadFeed() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Retry", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                state.posts.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "📝",
                                fontSize = 48.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No posts yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Be the first to share something!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToCreatePost,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Create Post", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(state.posts, key = { it.id }) { post ->
                            val isOwnPost = post.user_id == feedViewModel.currentUid
                            val isFollowing = post.user_id in state.followingIds
                            val isSaved = post.id in state.savedIds
                            PostItem(
                                post = post,
                                user = state.userMap[post.user_id],
                                isLiked = post.id in state.likedIds,
                                isSaved = isSaved,
                                isFollowing = isFollowing,
                                isOwnPost = isOwnPost,
                                onLike = { feedViewModel.toggleLike(post.id) },
                                onComment = { onNavigateToComments(post.id) },
                                onUserClick = { uid -> onNavigateToProfile(uid) },
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