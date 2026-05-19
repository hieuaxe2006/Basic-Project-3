package com.socialapp.ui.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    onLogout: () -> Unit // Thêm callback để thực hiện đăng xuất
) {
    val context = LocalContext.current
    LaunchedEffect(uid) { viewModel.loadProfile(uid) }

    val state = viewModel.state
    var showFollowersSheet by remember { mutableStateOf(false) }
    var showFollowingSheet by remember { mutableStateOf(false) }
    var showPasswordSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) } // State cho hộp thoại đăng xuất
    var showMetricsDialog by remember { mutableStateOf(false) }
    var postToShare by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(state.shareSuccess) {
        state.shareSuccess?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearShareState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.user?.username ?: "Trang cá nhân",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
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
                        // Nút Đăng xuất
                        IconButton(onClick = { showLogoutDialog = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Đăng xuất",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                        onUpdateMetrics = { showMetricsDialog = true },
                        viewModel = viewModel,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }

    // Hộp thoại xác nhận đăng xuất
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Đăng xuất") },
            text = { Text("Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng không?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Đăng xuất")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Các ModalBottomSheet giữ nguyên
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

    if (showMetricsDialog && state.user != null) {
        var heightText by remember { mutableStateOf(if(state.user.height > 0) state.user.height.toString() else "") }
        var weightText by remember { mutableStateOf(if(state.user.weight > 0) state.user.weight.toString() else "") }
        var bodyFatText by remember { mutableStateOf(if(state.user.body_fat > 0) state.user.body_fat.toString() else "") }
        var benchText by remember { mutableStateOf(if(state.user.bench_pr > 0) state.user.bench_pr.toString() else "") }
        var squatText by remember { mutableStateOf(if(state.user.squat_pr > 0) state.user.squat_pr.toString() else "") }
        var deadliftText by remember { mutableStateOf(if(state.user.deadlift_pr > 0) state.user.deadlift_pr.toString() else "") }

        AlertDialog(
            onDismissRequest = { showMetricsDialog = false },
            title = { Text("Cập nhật chỉ số Gymer") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Thông số cơ thể:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            label = { Text("Cao (cm)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Nặng (kg)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    OutlinedTextField(
                        value = bodyFatText,
                        onValueChange = { bodyFatText = it },
                        label = { Text("Tỉ lệ mỡ (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Kỷ lục cá nhân (PR):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = benchText,
                            onValueChange = { benchText = it },
                            label = { Text("Bench (kg)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = squatText,
                            onValueChange = { squatText = it },
                            label = { Text("Squat (kg)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    OutlinedTextField(
                        value = deadliftText,
                        onValueChange = { deadliftText = it },
                        label = { Text("Deadlift (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val h = heightText.toDoubleOrNull() ?: 0.0
                        val w = weightText.toDoubleOrNull() ?: 0.0
                        val bf = bodyFatText.toDoubleOrNull() ?: 0.0
                        val bp = benchText.toDoubleOrNull() ?: 0.0
                        val sq = squatText.toDoubleOrNull() ?: 0.0
                        val dl = deadliftText.toDoubleOrNull() ?: 0.0
                        viewModel.updateGymMetrics(h, w, bf, bp, sq, dl)
                        showMetricsDialog = false
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMetricsDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
    if (state.generatedWorkout != null) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { viewModel.clearGeneratedWorkout() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF00E676))
                    Spacer(Modifier.width(8.dp))
                    Text("Lịch tập đề xuất từ AI")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = state.generatedWorkout ?: "",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(state.generatedWorkout ?: ""))
                        Toast.makeText(context, "Đã sao chép lịch tập!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Sao chép")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearGeneratedWorkout() }) {
                    Text("Đóng")
                }
            }
        )
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
    onUpdateMetrics: () -> Unit,
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadAvatar(it, context) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            modifier = Modifier.size(120.dp),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
                            shadowElevation = 4.dp
                        ) {
                            if (user.avatar.isNotBlank()) {
                                AsyncImage(
                                    model = user.avatar,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Text(
                                        user.username.take(1).uppercase(),
                                        style = MaterialTheme.typography.displaySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        if (isOwnProfile) {
                            IconButton(
                                onClick = { avatarPicker.launch("image/*") },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        user.username,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (user.bio.isNotBlank()) {
                        Text(
                            user.bio,
                            modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("${user.followers_count}", "Người theo dõi", onFollowersClick)
                        StatItem("${user.following_count}", "Đang theo dõi", onFollowingClick)
                    }

                    Spacer(Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Chỉ số Gymer & Kỷ lục (PR)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (isOwnProfile) {
                                    Text(
                                        text = "Cập nhật",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { onUpdateMetrics() }
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                GymMetricItem(value = if (user.height > 0) "${user.height.toInt()} cm" else "--", label = "Chiều cao")
                                GymMetricItem(value = if (user.weight > 0) "${user.weight} kg" else "--", label = "Cân nặng")
                                GymMetricItem(value = if (user.body_fat > 0) "${user.body_fat}%" else "--", label = "Tỷ lệ mỡ")
                            }
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                GymMetricItem(value = if (user.bench_pr > 0) "${user.bench_pr} kg" else "--", label = "Bench Press")
                                GymMetricItem(value = if (user.squat_pr > 0) "${user.squat_pr} kg" else "--", label = "Squat")
                                GymMetricItem(value = if (user.deadlift_pr > 0) "${user.deadlift_pr} kg" else "--", label = "Deadlift")
                            }

                            if (isOwnProfile) {
                                Spacer(Modifier.height(16.dp))
                                val state = viewModel.state
                                Button(
                                    onClick = { viewModel.generateWorkoutPlan() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !state.isGeneratingWorkout
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (state.isGeneratingWorkout) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                            Spacer(Modifier.width(8.dp))
                                            Text("AI đang phân tích...", color = Color.White)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, null, tint = Color.Black)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Tạo Lịch Tập AI cá nhân hóa", color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (isOwnProfile) {
                        Button(
                            onClick = { viewModel.toggleEdit() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Chỉnh sửa trang cá nhân", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val friendStatus = viewModel.state.friendStatus
                            Button(
                                onClick = { if (friendStatus == "none") viewModel.sendFriendRequest() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (friendStatus == "friends") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                    contentColor = if (friendStatus == "friends") MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                val (icon, text) = when(friendStatus) {
                                    "friends" -> Icons.Default.Person to "Bạn bè"
                                    "requested" -> Icons.Default.PersonOutline to "Đã gửi"
                                    "pending_approval" -> Icons.Default.PersonAdd to "Chấp nhận"
                                    else -> Icons.Default.PersonAdd to "Thêm bạn"
                                }
                                Icon(icon, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(text, maxLines = 1)
                            }

                            Button(
                                onClick = onMessageClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Nhắn tin")
                            }
                        }
                    }
                }
            }
        }

        item {
            var selectedTabIndex by remember { mutableIntStateOf(0) }
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Bài viết", fontWeight = FontWeight.Bold) }
                )
                if (isOwnProfile) {
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Đã lưu", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (viewModel.state.isPostsLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        val posts = if (viewModel.state.postedPosts.isNotEmpty()) viewModel.state.postedPosts else emptyList()

        items(posts) { post ->
            PostThumbnail(
                post = post,
                onShareClick = { onShareClick(post) }
            )
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun StatItem(count: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Text(count, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
private fun PostThumbnail(post: Post, onShareClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                post.content,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (post.image_url.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = post.image_url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp), tint = Color.Red)
                Text(" ${post.like_count}", fontSize = 13.sp)
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(16.dp))
                Text(" ${post.comment_count}", fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onShareClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                }
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
        Text("Chỉnh sửa thông tin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Tên hiển thị") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Tiểu sử") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Hủy") }
            Button(onClick = { onSave(username, bio) }, modifier = Modifier.weight(1f), enabled = !isSaving) {
                if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White) else Text("Lưu")
            }
        }
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
            onValueChange = { searchQuery = it; onSearch(it) },
            placeholder = { Text("Tìm kiếm bạn bè...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            shape = RoundedCornerShape(24.dp)
        )
        Spacer(Modifier.height(16.dp))
        val displayList = if (searchQuery.isBlank()) friends else searchResults
        if (isLoading || isSearching) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn {
                items(displayList) { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { if (!isSharing) onFriendClick(user) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (user.avatar.isNotBlank()) {
                            AsyncImage(model = user.avatar, contentDescription = null, modifier = Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Surface(Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Box(contentAlignment = Alignment.Center) { Text(user.username.take(1).uppercase()) }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(user.username, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        if (isSharing) CircularProgressIndicator(Modifier.size(16.dp))
                    }
                }
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
        else LazyColumn {
            items(users) { user ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    if (user.avatar.isNotBlank()) AsyncImage(model = user.avatar, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    else Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Box(contentAlignment = Alignment.Center) { Text(user.username.take(1).uppercase()) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(user.username, fontWeight = FontWeight.Bold)
                }
            }
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
        Spacer(Modifier.height(24.dp))
        if (success) {
            Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(64.dp))
            Text("Thành công!", modifier = Modifier.padding(top = 16.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) { Text("Đóng") }
        } else {
            OutlinedTextField(value = current, onValueChange = { current = it }, label = { Text("Mật khẩu hiện tại") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = new, onValueChange = { new = it }, label = { Text("Mật khẩu mới") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Xác nhận mật khẩu") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            if (error != null) Text(error, color = Color.Red, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(32.dp))
            Button(onClick = { onSubmit(current, new) }, enabled = !isLoading && current.isNotBlank() && new == confirm, modifier = Modifier.fillMaxWidth()) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp)) else Text("Cập nhật")
            }
        }
    }
}

@Composable
private fun RowScope.GymMetricItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
    }
}