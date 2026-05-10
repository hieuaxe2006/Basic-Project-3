package com.socialapp.ui.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.socialapp.data.model.Post
import com.socialapp.data.model.User
import com.socialapp.ui.chat.UserAvatar
import kotlinx.coroutines.launch

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
    var barsVisible by remember { mutableStateOf(true) }
    var showShareSheet by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15f) barsVisible = false else if (available.y > 15f) barsVisible = true
                return Offset.Zero
            }
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = barsVisible,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it }
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "SocialApp",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { onNavigateToSearch("") },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = "Tìm kiếm", modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = onNavigateToChat,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "Tin nhắn", modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = barsVisible,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(64.dp)
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        selected = true,
                        onClick = { feedViewModel.loadFeed() },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(icon = { Icon(Icons.Default.Explore, null) }, selected = false, onClick = onNavigateToExplore)
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AddBox, null, modifier = Modifier.size(28.dp)) },
                        selected = false,
                        onClick = onNavigateToCreatePost
                    )
                    NavigationBarItem(icon = { Icon(Icons.Default.Notifications, null) }, selected = false, onClick = { })
                    NavigationBarItem(icon = { Icon(Icons.Default.Menu, null) }, selected = false, onClick = onNavigateToSettings)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).nestedScroll(nestedScrollConnection)) {
            if (state.isLoading && state.posts.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        QuickCreatePostBar(onNavigateToProfile, onNavigateToCreatePost, state.currentUser)
                    }
                    
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
                            onFollow = { feedViewModel.toggleFollow(post.user_id) },
                            onShare = {
                                selectedPost = post
                                showShareSheet = true
                                feedViewModel.loadFriends()
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (showShareSheet) {
                ModalBottomSheet(
                    onDismissRequest = { 
                        showShareSheet = false 
                        feedViewModel.clearShareState()
                    },
                    sheetState = sheetState
                ) {
                    SharePostSheet(
                        state = state,
                        onShare = { friend ->
                            selectedPost?.let { post ->
                                feedViewModel.sharePost(post, friend)
                            }
                        },
                        onSearch = { feedViewModel.searchUsers(it) }
                    )
                }
            }

            // Success message snackbar-like UI
            state.shareSuccess?.let { msg ->
                LaunchedEffect(msg) {
                    scope.launch {
                        // In a real app we'd use SnackbarHostState, 
                        // but for brevity we'll just auto-dismiss state after 2s
                        kotlinx.coroutines.delay(2000)
                        feedViewModel.clearShareState()
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.8f)
                ) {
                    Text(
                        msg,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SharePostSheet(
    state: FeedState,
    onShare: (User) -> Unit,
    onSearch: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .heightIn(max = 400.dp)
    ) {
        Text(
            "Gửi đến",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onSearch(it)
            },
            placeholder = { Text("Tìm kiếm bạn bè") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )
        
        Spacer(Modifier.height(16.dp))
        
        if (state.isListLoading || state.isSearching) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val listToShow = if (searchQuery.isNotBlank()) state.searchResults else state.friendsList
            
            if (listToShow.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy người dùng nào", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listToShow) { friend ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShare(friend) }
                                .padding(vertical = 4.dp)
                        ) {
                            UserAvatar(user = friend, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                friend.username,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { onShare(friend) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Text("Gửi")
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun QuickCreatePostBar(onProfile: (String?) -> Unit, onCreate: () -> Unit, user: com.socialapp.data.model.User?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onProfile(null) },
                contentAlignment = Alignment.Center
            ) {
                if (user?.avatar?.isNotBlank() == true) {
                    // Coil AsyncImage would go here
                    AsyncImage(
                        model = user.avatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(user?.username?.take(1)?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
                }
            }
            
            Surface(
                onClick = onCreate,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .height(36.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Bạn đang nghĩ gì?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
            
            IconButton(onClick = onCreate) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Color(0xFF45BD62))
            }
        }
    }
}
