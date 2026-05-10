package com.socialapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.socialapp.data.model.Message
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    otherUid: String,
    otherName: String,
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val state = viewModel.state
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(otherUid) { 
        viewModel.startListening(otherUid) 
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(state.messages.size - 1) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // User Avatar placeholder or logic if available
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(otherName.take(1).uppercase(), fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(otherName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Đang hoạt động", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Filled.Call, null, tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = {}) { Icon(Icons.Filled.Videocam, null, tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = {}) { Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.primary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}) { Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = {}) { Icon(Icons.Filled.CameraAlt, null, tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = {}) { Icon(Icons.Filled.Photo, null, tint = MaterialTheme.colorScheme.primary) }
                    
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Tin nhắn") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    
                    if (input.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.sendMessage(otherUid, input)
                                input = ""
                            },
                            enabled = !state.isSending
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = {}) { Icon(Icons.Filled.ThumbUp, null, tint = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            itemsIndexed(state.messages, key = { _, msg -> msg.id }) { index, message ->
                val isOwn = message.sender_id == viewModel.currentUid
                val showAvatar = !isOwn && (index == 0 || state.messages[index-1].sender_id != message.sender_id)
                
                MessageBubble(
                    message = message,
                    isOwn = isOwn,
                    showAvatar = showAvatar
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isOwn: Boolean, showAvatar: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isOwn) {
            if (showAvatar) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(message.sender_id.take(1), fontSize = 10.sp)
                    }
                }
            } else {
                Spacer(Modifier.width(28.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isOwn) 18.dp else 4.dp,
                bottomEnd = if (isOwn) 4.dp else 18.dp
            ),
            color = if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.content,
                    color = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )
                
                if (message.content.contains("http")) {
                    val imageUrl = message.content.substringAfterLast("\n").trim()
                    if (imageUrl.startsWith("http")) {
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
