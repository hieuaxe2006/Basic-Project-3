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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.socialapp.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatListViewModel,
    onBack: () -> Unit,
    onOpenChat: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val state = viewModel.state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Đoạn chat", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Camera action */ }) {
                        Icon(Icons.Default.CameraAlt, "Camera")
                    }
                    IconButton(onClick = { /* New message */ }) {
                        Icon(Icons.Default.Edit, "New Message")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchUsers(it)
                },
                placeholder = { Text("Tìm kiếm") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Search Results
                if (searchQuery.isNotBlank()) {
                    item {
                        Text(
                            "Kết quả tìm kiếm",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(state.searchResults, key = { "search_${it.id}" }) { user ->
                        UserChatItem(user) {
                            onOpenChat(user.id, user.username)
                        }
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                }

                // Friend Requests Section
                if (state.pendingRequests.isNotEmpty() && searchQuery.isBlank()) {
                    item {
                        Text(
                            "Lời mời kết bạn",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    items(state.pendingRequests) { (request, user) ->
                        FriendRequestItem(user = user, onAccept = { viewModel.acceptRequest(request.id) })
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // Story/Active Users Row (Messenger Style)
                if (searchQuery.isBlank()) {
                    item {
                        ActiveUsersRow(state.partners)
                    }
                }

                // Chat History
                if (state.isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (state.partners.isEmpty() && searchQuery.isBlank()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                            Text("Chưa có cuộc trò chuyện nào", color = Color.Gray)
                        }
                    }
                } else {
                    items(state.partners, key = { it.id }) { user ->
                        UserChatItem(user) {
                            onOpenChat(user.id, user.username)
                        }
                    }
                }
            }
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
            Text("Muốn kết bạn với bạn", fontSize = 12.sp, color = Color.Gray)
        }
        Button(
            onClick = onAccept,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Chấp nhận")
        }
    }
}

@Composable
private fun ActiveUsersRow(users: List<User>) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(users) { user ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    UserAvatar(user, size = 56.dp)
                    // Active Status Dot
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color.White, CircleShape)
                            .padding(2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF4CAF50), CircleShape))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    user.username.split(" ").first(),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(56.dp)
                )
            }
        }
    }
}

@Composable
private fun UserChatItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(user, size = 56.dp)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                user.username,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                "Đã gửi một tin nhắn · 10 phút", // Placeholder for last message
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UserAvatar(user: User, size: androidx.compose.ui.unit.Dp) {
    if (user.avatar.isNotBlank()) {
        AsyncImage(
            model = user.avatar,
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    user.username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
