package com.socialapp.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import com.socialapp.utils.t
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
    var isSearchExpanded by remember { mutableStateOf(false) }

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

    val listState = rememberLazyListState()

    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(isAtTop) {
        if (isAtTop) {
            barsVisible = true
        }
    }

    val animatedHeaderHeight by animateDpAsState(
        targetValue = if (barsVisible) 56.dp else 0.dp,
        animationSpec = tween(300),
        label = "HeaderHeight"
    )
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > 0 && lastVisibleItemIndex >= totalItemsNumber - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            feedViewModel.loadNextPage()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading && state.posts.isEmpty()) {
                val brush = shimmerBrush()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 56.dp),
                    userScrollEnabled = false
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(brush)
                        )
                    }
                    item { StorySkeletonSection(brush = brush) }
                    items(3) { PostSkeletonItem(brush = brush) }
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = {
                        feedViewModel.loadFeed(isRefresh = true)
                        feedViewModel.loadStories()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 56.dp)
                    ) {
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
                                onUserClick = { onNavigateToProfile(post.user_id) },
                                onSave = { feedViewModel.toggleSave(post.id) },
                                onFollow = { feedViewModel.toggleFollow(post.user_id) },
                                onShare = { selectedPost = post; showShareSheet = true; feedViewModel.loadFriends() },
                                onUpdateVisibility = { isPrivate -> feedViewModel.updatePostVisibility(post.id, isPrivate) },
                                onUpdateCommentsDisabled = { disabled -> feedViewModel.updatePostCommentsDisabled(post.id, disabled) },
                                onDeletePost = { feedViewModel.deletePost(post.id) }
                            )
                        }
                    }
                }
            }

            val alpha by animateFloatAsState(
                targetValue = if (barsVisible) 1f else 0f,
                animationSpec = tween(if (barsVisible) 250 else 150),
                label = "HeaderAlpha"
            )

            Surface(
                shadowElevation = if (animatedHeaderHeight > 0.dp) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(animatedHeaderHeight)
                    .clipToBounds()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { this.alpha = alpha }
                ) {
                    AnimatedVisibility(
                        visible = isSearchExpanded,
                        enter = slideInVertically { -it } + fadeIn(),
                        exit = slideOutVertically { -it } + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { isSearchExpanded = false; searchQuery = "" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(t("search_hint")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        onNavigateToSearch(searchQuery)
                                    }
                                }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = !isSearchExpanded,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GymHub",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        scope.launch {
                                            listState.animateScrollToItem(0)
                                        }
                                        feedViewModel.loadFeed(isRefresh = true)
                                        feedViewModel.loadStories()
                                    }
                            )
                            BadgedBox(
                                badge = {
                                    if (state.unreadNotificationCount > 0) {
                                        Badge { Text(state.unreadNotificationCount.toString()) }
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = onNavigateToNotifications,
                                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(imageVector = Icons.Outlined.Notifications, contentDescription = "Notifications", modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings", modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            IconButton(
                                onClick = { isSearchExpanded = true },
                                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search", modifier = Modifier.size(24.dp))
                            }
                        }
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
fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

@Composable
fun StorySkeletonSection(brush: Brush) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(5) {
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 190.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
fun PostSkeletonItem(brush: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(brush)
                )
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
            val storyUser = userMap[story.userId] ?: if (story.userId == currentUser?.id) currentUser else null
            StoryItem(story, storyUser, onStoryClick)
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
                    t("create_story"),
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
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                UserAvatar(
                    user = user ?: User(id = story.userId, username = "Unknown"),
                    size = 32.dp
                )
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
            UserAvatar(user = user ?: User(id = "", username = "?"), size = 40.dp, onClick = { onProfile(null) })
            Surface(onClick = onCreate, modifier = Modifier.weight(1f).padding(horizontal = 12.dp).height(36.dp), shape = RoundedCornerShape(20.dp), color = Color.Transparent, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(t("quick_post_hint"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
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