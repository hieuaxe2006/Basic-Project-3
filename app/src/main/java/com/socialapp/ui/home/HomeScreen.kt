package com.socialapp.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.socialapp.data.model.Post
import com.socialapp.data.model.Story
import com.socialapp.data.model.User
import com.socialapp.ui.chat.UserAvatar
import kotlinx.coroutines.delay
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
    onNavigateToCreateStory: () -> Unit,
    onNavigateToViewStory: (String) -> Unit,
    onNavigateToNotifications: () -> Unit,
    feedViewModel: FeedViewModel = viewModel()
) {
    val state = feedViewModel.state
    var barsVisible by remember { mutableStateOf(true) }
    var showShareSheet by remember { mutableStateOf(false) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15f) barsVisible = false
                else if (available.y > 15f) barsVisible = true
                return Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        topBar = {
            AnimatedVisibility(
                visible = barsVisible,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it }
            ) {
                Surface(shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                    Column {
                        TopAppBar(
                            title = {
                                Text("GymHub", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                            },
                            actions = {
                                BadgedBox(
                                    badge = {
                                        if (state.unreadNotificationCount > 0) {
                                            Badge { Text(state.unreadNotificationCount.toString()) }
                                        }
                                    }
                                ) {
                                    IconButton(
                                        onClick = onNavigateToNotifications,
                                        modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    ) {
                                        Icon(imageVector = Icons.Outlined.Notifications, contentDescription = "Notifications", modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(Modifier.width(15.dp)) // Chỉnh khoảng cách thành 15dp
                                IconButton(
                                    onClick = { onNavigateToSearch("") },
                                    modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                            }
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Tìm gymer, bài tập...") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { if (searchQuery.isNotBlank()) onNavigateToSearch(searchQuery) })
                        )
                    }
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(visible = barsVisible, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
                    NavigationBarItem(
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home") },
                        selected = true,
                        onClick = { feedViewModel.loadFeed() }
                    )
                    NavigationBarItem(
                        icon = { Icon(imageVector = Icons.Default.Explore, contentDescription = null) },
                        label = { Text("Explore") },
                        selected = false,
                        onClick = onNavigateToExplore
                    )
                    NavigationBarItem(
                        icon = { Icon(imageVector = Icons.Default.AddCircle, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp), contentDescription = null) },
                        label = { Text("Post") },
                        selected = false,
                        onClick = onNavigateToCreatePost
                    )
                    NavigationBarItem(
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (state.unreadChatCount > 0) {
                                        Badge(containerColor = Color.Red) {
                                            Text(state.unreadChatCount.toString(), color = Color.White)
                                        }
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Chat, contentDescription = "Chat")
                            }
                        },
                        label = { Text("Chat") },
                        selected = false,
                        onClick = onNavigateToChat
                    )
                    NavigationBarItem(
                        icon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                        label = { Text("Profile") },
                        selected = false,
                        onClick = { onNavigateToProfile(null) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading && state.posts.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { QuickCreatePostBar(onNavigateToProfile, onNavigateToCreatePost, state.currentUser) }
                    item { StorySection(state.stories, state.currentUser, state.userMap, onCreateStory = onNavigateToCreateStory, onStoryClick = onNavigateToViewStory) }
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
                            onShare = { selectedPost = post; showShareSheet = true; feedViewModel.loadFriends() }
                        )
                    }
                }
            }

            if (showShareSheet) {
                ModalBottomSheet(onDismissRequest = { showShareSheet = false; feedViewModel.clearShareState() }, sheetState = sheetState) {
                    SharePostSheet(state = state, onShare = { friend -> selectedPost?.let { post -> feedViewModel.sharePost(post, friend) } }, onSearch = { feedViewModel.searchUsers(it) })
                }
            }

            state.shareSuccess?.let { msg ->
                LaunchedEffect(msg) { delay(2000); feedViewModel.clearShareState() }
                Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.8f)) {
                    Text(msg, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun StorySection(stories: List<Story>, currentUser: User?, userMap: Map<String, User>, onCreateStory: () -> Unit, onStoryClick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { CreateStoryItem(currentUser, onCreateStory) }
        items(stories) { story ->
            StoryItem(story, userMap[story.userId], onStoryClick)
        }
    }
}

@Composable
fun CreateStoryItem(user: User?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(width = 110.dp, height = 190.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (user?.avatar?.isNotBlank() == true) {
                AsyncImage(
                    model = user.avatar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(130.dp).background(Color.LightGray), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Gray)
                }
            }
            
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(60.dp).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Tạo tin",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Surface(
                modifier = Modifier.align(Alignment.Center).offset(y = 35.dp).size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.padding(4.dp))
            }
        }
    }
}

@Composable
fun StoryItem(story: Story, user: User?, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier.size(width = 110.dp, height = 190.dp).clickable { onClick(story.id) },
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (story.type == "text") {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(android.graphics.Color.parseColor(story.backgroundColor))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.text,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                AsyncImage(
                    model = story.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Gradient overlay for better text readability
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                        startY = 300f
                    )
                )
            )

            // User Avatar at top left
            Surface(
                modifier = Modifier.padding(8.dp).size(36.dp),
                shape = CircleShape,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                if (user?.avatar?.isNotBlank() == true) {
                    AsyncImage(model = user.avatar, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Gray), contentAlignment = Alignment.Center) {
                        Text(user?.username?.take(1)?.uppercase() ?: "?", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // Username at bottom
            Text(
                text = user?.username ?: "Unknown",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            )
        }
    }
}

@Composable
fun QuickCreatePostBar(onProfile: (String?) -> Unit, onCreate: () -> Unit, user: User?) {
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onProfile(null) }, contentAlignment = Alignment.Center) {
                if (user?.avatar?.isNotBlank() == true) {
                    AsyncImage(model = user.avatar, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Text(user?.username?.take(1)?.uppercase() ?: "?", fontWeight = FontWeight.Bold)
                }
            }
            Surface(onClick = onCreate, modifier = Modifier.weight(1f).padding(horizontal = 12.dp).height(36.dp), shape = RoundedCornerShape(20.dp), color = Color.Transparent, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Hôm nay tập gì?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
            IconButton(onClick = onCreate) {
                Icon(imageVector = Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Color(0xFF45BD62))
            }
        }
    }
}

@Composable
fun SharePostSheet(state: FeedState, onShare: (User) -> Unit, onSearch: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 400.dp)) {
        Text("Gửi đến", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it; onSearch(it) }, placeholder = { Text("Tìm kiếm bạn bè") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) })
        Spacer(Modifier.height(16.dp))
        if (state.isListLoading || state.isSearching) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val listToShow = if (searchQuery.isNotBlank()) state.searchResults else state.friendsList
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listToShow) { friend ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onShare(friend) }.padding(vertical = 4.dp)) {
                        UserAvatar(user = friend, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(friend.username, modifier = Modifier.weight(1f))
                        Button(onClick = { onShare(friend) }) { Text("Gửi") }
                    }
                }
            }
        }
    }
}
