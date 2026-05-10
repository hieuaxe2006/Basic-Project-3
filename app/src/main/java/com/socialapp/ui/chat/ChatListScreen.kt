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
                placeholder = { Text("Tìm kiếm", fontSize = 15.sp) },
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
                // Story/Active Users Row (Messenger Style)
                if (searchQuery.isBlank()) {
                    item {
                        ActiveUsersRow(
                            currentUser = state.currentUser,
                            partners = state.partners.map { it.user },
                            onNoteClick = { showNoteDialog = true }
                        )
                    }
                }

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
                        UserChatItem(ChatPartner(user)) {
                            onOpenChat(user.id, user.username)
                        }
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant) }
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
            title = { Text("Ghi chú của bạn") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Chia sẻ ý nghĩ...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateNote(noteText)
                    showNoteDialog = false
                }) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Chấp nhận")
        }
    }
}

@Composable
private fun ActiveUsersRow(
    currentUser: User?,
    partners: List<User>,
    onNoteClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // You (Your Note) item
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (currentUser?.note?.isNotBlank() == true) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    currentUser.note,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Spacer(Modifier.height(20.dp))
                        }
                        
                        Box {
                            if (currentUser != null) {
                                UserAvatar(currentUser, size = 56.dp, onClick = onNoteClick)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { onNoteClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = Color.Gray)
                                }
                            }
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
                                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Ghi chú", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        }

        items(partners) { user ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
                Spacer(Modifier.height(20.dp)) // Alignment with current user's avatar
                Box {
                    UserAvatar(user, size = 56.dp)
                    // Active Status Dot
                    Box(
                        modifier = Modifier
                            .size(16.dp)
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
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun UserChatItem(partner: ChatPartner, onClick: () -> Unit) {
    val user = partner.user
    val lastMsg = partner.lastMessage
    
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
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            val subtitle = if (lastMsg != null) {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(lastMsg.timestamp.toDate())
                val prefix = if (lastMsg.sender_id == (FirebaseAuth.getInstance().currentUser?.uid ?: "")) "Bạn: " else ""
                "$prefix${lastMsg.content} · $time"
            } else {
                "Bắt đầu cuộc trò chuyện"
            }

            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UserAvatar(user: User, size: androidx.compose.ui.unit.Dp, onClick: (() -> Unit)? = null) {
    val modifier = Modifier
        .size(size)
        .clip(CircleShape)
        .let { if (onClick != null) it.clickable { onClick() } else it }

    if (user.avatar.isNotBlank()) {
        AsyncImage(
            model = user.avatar,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = modifier,
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
