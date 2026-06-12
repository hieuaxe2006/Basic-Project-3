package com.socialapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.socialapp.data.model.User
import com.socialapp.data.repository.ChatPartner
import com.socialapp.utils.t
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onBack: () -> Unit,
    onOpenChat: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val state = viewModel.state
    var showNoteDialog by remember { mutableStateOf(false) }

    // Tự động load lại danh sách khi vào màn hình để cập nhật tín hiệu tin nhắn mới nhất
    LaunchedEffect(Unit) {
        viewModel.loadChatPartners()
        viewModel.loadPendingRequests()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(t("chat_list_title"), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { /* Camera action */ },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(36.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, "Camera", modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { /* New message */ },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).size(36.dp)
                    ) {
                        Icon(Icons.Default.Edit, "New Message", modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchUsers(it)
                },
                placeholder = { Text(t("search_hint"), fontSize = 15.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (searchQuery.isBlank()) {
                    item {
                        ActiveUsersRow(
                            currentUser = state.currentUser,
                            partners = state.partners.map { it.user },
                            onNoteClick = { showNoteDialog = true }
                        )
                    }
                }

                if (searchQuery.isNotBlank()) {
                    item {
                        Text(
                            t("search_hint"), // Hoặc tạo key riêng cho "Kết quả tìm kiếm"
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(state.searchResults, key = { "search_${it.id}" }) { user ->
                        UserChatItem(ChatPartner(user)) {
                            onOpenChat(user.id, user.username)
                        }
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant) }
                }

                if (state.pendingRequests.isNotEmpty() && searchQuery.isBlank()) {
                    item {
                        Text(
                            t("friend_requests"),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    items(state.pendingRequests) { (request, user) ->
                        FriendRequestItem(user = user, onAccept = { viewModel.acceptRequest(request.id) })
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                if (state.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (state.partners.isEmpty() && searchQuery.isBlank()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                            Text(t("no_chats"), color = Color.Gray)
                        }
                    }
                } else {
                    items(state.partners, key = { it.user.id }) { partner ->
                        UserChatItem(partner) {
                            onOpenChat(partner.user.id, partner.user.username)
                        }
                    }
                }
            }
        }
    }

    if (showNoteDialog) {
        var noteText by remember { mutableStateOf(state.currentUser?.note ?: "") }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text(t("workout_log")) }, // Sử dụng key phù hợp cho tiêu đề note
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text(t("quick_post_hint")) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateNote(noteText)
                    showNoteDialog = false
                }) {
                    Text(t("save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text(t("cancel"))
                }
            }
        )
    }
}

@Composable
private fun UserChatItem(partner: ChatPartner, onClick: () -> Unit) {
    val user = partner.user
    val lastMsg = partner.lastMessage
    val isUnread = partner.unreadCount > 0 // Tín hiệu tin nhắn mới

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(user, size = 60.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                user.username,
                // Nếu chưa đọc, tên sẽ hiện Đậm (Bold)
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val subtitle = if (lastMsg != null) {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(lastMsg.timestamp.toDate())
                // Key dịch cho tiền tố "Bạn: "
                val prefix = if (lastMsg.sender_id == (FirebaseAuth.getInstance().currentUser?.uid ?: "")) "You: " else ""
                "$prefix${lastMsg.content} · $time"
            } else {
                "..." // Bắt đầu cuộc trò chuyện
            }

            Text(
                subtitle,
                // Nếu chưa đọc, tin nhắn sẽ hiện màu đậm hơn
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // TÍN HIỆU CHẤM XANH: Thông báo có tin nhắn mới
        if (isUnread) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun FriendRequestItem(user: User, onAccept: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(user, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.username, fontWeight = FontWeight.Bold)
            Text(t("friend_requests"), fontSize = 12.sp, color = Color.Gray)
        }
        Button(
            onClick = onAccept,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(t("accept"), fontSize = 12.sp)
        }
    }
}

@Composable
private fun ActiveUserItem(
    user: User,
    label: String,
    isCurrentUser: Boolean,
    onNoteClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(68.dp)
    ) {
        // Upper area: note bubble (or empty spacer) with fixed height of 32.dp
        Box(
            modifier = Modifier
                .height(32.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (user.note.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = if (onNoteClick != null) Modifier.clickable { onNoteClick() } else Modifier
                ) {
                    Text(
                        text = user.note,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Avatar area
        Box(contentAlignment = Alignment.Center) {
            UserAvatar(user, size = 56.dp, onClick = onNoteClick)
            if (isCurrentUser) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color.White, CircleShape)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Label/Username area
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isCurrentUser) Color.Gray else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActiveUsersRow(currentUser: User?, partners: List<User>, onNoteClick: () -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ActiveUserItem(
                user = currentUser ?: User(username = "You"),
                label = t("workout_log"), // Hoặc key dịch cho "Ghi chú"
                isCurrentUser = true,
                onNoteClick = onNoteClick
            )
        }
        items(partners) { user ->
            ActiveUserItem(
                user = user,
                label = user.username.split(" ").firstOrNull() ?: "",
                isCurrentUser = false,
                onNoteClick = null
            )
        }
    }
}

fun getAvatarBgColor(userId: String): Color {
    val colors = listOf(
        Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD),
        Color(0xFF7986CB), Color(0xFF64B5F6), Color(0xFF4FC3F7), Color(0xFF4DB6AC),
        Color(0xFF81C784), Color(0xFFAED581), Color(0xFFFFB74D), Color(0xFFFF8A65),
        Color(0xFFA1887F), Color(0xFF90A4AE)
    )
    if (userId.isBlank()) return Color(0xFF90A4AE)
    val index = Math.abs(userId.hashCode()) % colors.size
    return colors[index]
}

@Composable
fun UserAvatar(user: User, size: androidx.compose.ui.unit.Dp, onClick: (() -> Unit)? = null) {
    val modifier = Modifier.size(size).clip(CircleShape).let { if (onClick != null) it.clickable { onClick() } else it }
    if (user.avatar.isNotBlank()) {
        AsyncImage(model = user.avatar, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        val bgColor = remember(user.id) { getAvatarBgColor(user.id) }
        Surface(modifier = modifier, shape = CircleShape, color = bgColor) {
            Box(contentAlignment = Alignment.Center) {
                Text(user.username.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}