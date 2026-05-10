package com.socialapp.ui.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.socialapp.data.model.Post
import com.socialapp.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    uid: String? = null,
    onBack: (() -> Unit)? = null,
    onNavigateToChat: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    LaunchedEffect(uid) { viewModel.loadProfile(uid) }

    val state = viewModel.state
    var showFollowersSheet by remember { mutableStateOf(false) }
    var showFollowingSheet by remember { mutableStateOf(false) }
    var showPasswordSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var postToShare by remember { mutableStateOf<Post?>(null) }

    // Thông báo khi chia sẻ thành công
    LaunchedEffect(state.shareSuccess) {
        state.shareSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearShareState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trang cá nhân") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    if (viewModel.isOwnProfile && !state.isEditing) {
                        IconButton(onClick = { showPasswordSheet = true }) {
                            Icon(Icons.Default.Lock, "Đổi mật khẩu")
                        }
                        IconButton(onClick = { viewModel.toggleEdit() }) {
                            Icon(Icons.Default.Edit, "Chỉnh sửa")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                }
            }
            state.user != null -> {
                if (state.isEditing) {
                    EditProfileContent(
                        user = state.user,
                        isSaving = state.isSaving,
                        onSave = { username, bio -> viewModel.saveProfile(username, bio) },
                        onCancel = { viewModel.toggleEdit() },
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    ProfileContent(
                        user = state.user,
                        isOwnProfile = viewModel.isOwnProfile,
                        onFollowersClick = {
                            showFollowersSheet = true
                            viewModel.loadFollowers()
                        },
                        onFollowingClick = {
                            showFollowingSheet = true
                            viewModel.loadFollowing()
                        },
                        onMessageClick = {
                            onNavigateToChat(state.user.id, state.user.username)
                        },
                        onShareClick = { post ->
                            postToShare = post
                            showShareSheet = true
                            viewModel.loadFriends()
                        },
                        viewModel = viewModel,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }

    if (showFollowersSheet) {
        ModalBottomSheet(onDismissRequest = { showFollowersSheet = false }) {
            UserListContent(
                title = "Người theo dõi",
                users = state.followersList,
                isLoading = state.isListLoading
            )
        }
    }
    if (showFollowingSheet) {
        ModalBottomSheet(onDismissRequest = { showFollowingSheet = false }) {
            UserListContent(
                title = "Đang theo dõi",
                users = state.followingList,
                isLoading = state.isListLoading
            )
        }
    }
    if (showShareSheet) {
        ModalBottomSheet(onDismissRequest = { showShareSheet = false }) {
            ShareSheetContent(
                friends = state.friendsList,
                searchResults = state.searchResults,
                isLoading = state.isListLoading,
                isSearching = state.isSearching,
                isSharing = state.isSharing,
                onSearch = { viewModel.searchUsers(it) },
                onFriendClick = { friend ->
                    postToShare?.let { post ->
                        viewModel.sharePost(post, friend)
                    }
                    showShareSheet = false
                }
            )
        }
    }
    if (showPasswordSheet) {
        ModalBottomSheet(onDismissRequest = {
            showPasswordSheet = false
            viewModel.clearPasswordState()
        }) {
            ChangePasswordContent(
                isLoading = state.isChangingPassword,
                success = state.passwordChangeSuccess,
                error = state.passwordChangeError,
                onSubmit = { current, new -> viewModel.changePassword(current, new) },
                onDismiss = {
                    showPasswordSheet = false
                    viewModel.clearPasswordState()
                }
            )
        }
    }
}

@Composable
private fun ProfileContent(
    user: User,
    isOwnProfile: Boolean,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onMessageClick: () -> Unit,
    onShareClick: (Post) -> Unit,
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadAvatar(it, context) }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        
        // Avatar
        Box(contentAlignment = Alignment.BottomEnd) {
            if (user.avatar.isNotBlank()) {
                AsyncImage(
                    model = user.avatar,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(user.username.take(1).uppercase(), style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }
            if (isOwnProfile) {
                IconButton(
                    onClick = { avatarPicker.launch("image/*") },
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(user.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(user.email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        if (user.bio.isNotBlank()) {
            Text(user.bio, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))

        // Actions Row
        if (!isOwnProfile) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val friendStatus = viewModel.state.friendStatus
                    Button(
                        onClick = { 
                            if (friendStatus == "none") viewModel.sendFriendRequest()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (friendStatus == "friends") Color(0xFFE4E6EB) else MaterialTheme.colorScheme.primary,
                            contentColor = if (friendStatus == "friends") Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        val (icon, text) = when(friendStatus) {
                            "friends" -> Icons.Default.Person to "Bạn bè"
                            "requested" -> Icons.Default.PersonOutline to "Đã gửi yêu cầu"
                            "pending_approval" -> Icons.Default.PersonAdd to "Chấp nhận"
                            else -> Icons.Default.PersonAdd to "Thêm bạn bè"
                        }
                        Icon(icon, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text)
                    }

                    Button(
                        onClick = onMessageClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE4E6EB), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Nhắn tin")
                    }
                }
                
                // Follow Button
                val isFollowing = viewModel.state.isFollowing
                Button(
                    onClick = { viewModel.toggleFollow() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color(0xFFE4E6EB) else MaterialTheme.colorScheme.primary,
                        contentColor = if (isFollowing) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(if (isFollowing) Icons.Default.Check else Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isFollowing) "Đang theo dõi" else "Theo dõi")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            StatItem("Người theo dõi", user.followers_count, onFollowersClick)
            StatItem("Đang theo dõi", user.following_count, onFollowingClick)
        }

        Spacer(Modifier.height(16.dp))
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Bài viết") })
            if (isOwnProfile) Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Đã lưu") })
        }

        Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
            val posts = if (selectedTabIndex == 0) viewModel.state.postedPosts else viewModel.state.savedPosts
            if (viewModel.state.isPostsLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (posts.isEmpty()) {
                Text("Chưa có bài viết nào", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn {
                    items(posts) { post -> 
                        PostThumbnail(
                            post = post, 
                            onShareClick = { onShareClick(post) }
                        ) 
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, count: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Text("$count", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
private fun ShareSheetContent(
    friends: List<User>,
    searchResults: List<User>,
    isLoading: Boolean,
    isSearching: Boolean,
    isSharing: Boolean,
    onSearch: (String) -> Unit,
    onFriendClick: (User) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(16.dp)) {
        Text("Gửi bài viết", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                onSearch(it)
            },
            placeholder = { Text("Tìm kiếm bạn bè hoặc người khác...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = ""; onSearch("") }) {
                        Icon(Icons.Default.Clear, null)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        
        Spacer(Modifier.height(16.dp))
        
        val displayList = if (searchQuery.isBlank()) friends else searchResults
        val showLoading = isLoading || isSearching

        if (showLoading && displayList.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (displayList.isEmpty() && !showLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                Text(if (searchQuery.isEmpty()) "Chưa có bạn bè để chia sẻ" else "Không tìm thấy người dùng") 
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(displayList) { user ->
                    UserShareItem(
                        user = user,
                        isSharing = isSharing,
                        onClick = { onFriendClick(user) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserShareItem(user: User, isSharing: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isSharing) onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (user.avatar.isNotBlank()) {
            AsyncImage(
                model = user.avatar,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(contentAlignment = Alignment.Center) { Text(user.username.take(1).uppercase()) }
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(user.username, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        if (isSharing) CircularProgressIndicator(Modifier.size(16.dp))
        else Icon(Icons.AutoMirrored.Filled.Chat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun PostThumbnail(post: Post, onShareClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(post.content, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (post.image_url.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = post.image_url, 
                    contentDescription = null, 
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)), 
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp), tint = Color.Red)
                Text(" ${post.like_count}", fontSize = 12.sp)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(16.dp))
                Text(" ${post.comment_count}", fontSize = 12.sp)
                
                Spacer(Modifier.width(16.dp))
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EditProfileContent(
    user: User,
    isSaving: Boolean,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember(user) { mutableStateOf(user.username) }
    var bio by remember(user) { mutableStateOf(user.bio) }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Tên người dùng") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Tiểu sử") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Hủy") }
            Button(onClick = { onSave(username, bio) }, modifier = Modifier.weight(1f), enabled = !isSaving) {
                if (isSaving) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Lưu")
            }
        }
    }
}

@Composable
private fun UserListContent(title: String, users: List<User>, isLoading: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f).padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else if (users.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Trống") }
        else LazyColumn { items(users) { user -> UserItem(user) } }
    }
}

@Composable
private fun UserItem(user: User) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (user.avatar.isNotBlank()) AsyncImage(model = user.avatar, contentDescription = null, modifier = Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        else Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
            Box(contentAlignment = Alignment.Center) { Text(user.username.take(1).uppercase()) }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(user.username, fontWeight = FontWeight.Bold)
            if (user.bio.isNotBlank()) Text(user.bio, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1)
        }
    }
}

@Composable
private fun ChangePasswordContent(isLoading: Boolean, success: Boolean, error: String?, onSubmit: (String, String) -> Unit, onDismiss: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Đổi mật khẩu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (success) {
            Text("✅ Đổi mật khẩu thành công!", color = MaterialTheme.colorScheme.primary)
            Button(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) { Text("Đóng") }
        } else {
            OutlinedTextField(value = current, onValueChange = { current = it }, label = { Text("Mật khẩu hiện tại") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = new, onValueChange = { new = it }, label = { Text("Mật khẩu mới") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Xác nhận mật khẩu mới") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { onSubmit(current, new) }, enabled = !isLoading && current.isNotBlank() && new.length >= 6 && new == confirm, modifier = Modifier.fillMaxWidth()) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Đổi mật khẩu")
            }
        }
    }
}
