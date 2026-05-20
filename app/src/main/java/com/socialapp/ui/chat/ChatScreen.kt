package com.socialapp.ui.chat

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onBack: () -> Unit,
    onShowInfo: (String, String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val state = viewModel.state
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.sendImage(otherUid, it, context) }
    }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onShowInfo(otherUid, otherName) }
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(otherName.take(1).uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(otherName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                            if (!state.isBlockedByMe && !state.isBlockedByOther) {
                                Text("Đang hoạt động", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { onShowInfo(otherUid, otherName) }) {
                        Icon(Icons.Filled.Info, "Thông tin", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
                Column {
                    if (state.isUploading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    
                    if (state.isBlockedByMe || state.isBlockedByOther) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.isBlockedByMe) "Bạn đã chặn người dùng này. Bỏ chặn để nhắn tin." else "Người dùng này hiện không thể nhận tin nhắn.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Icon(Icons.Filled.Photo, "Chọn ảnh", tint = MaterialTheme.colorScheme.primary) 
                            }
                            
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                placeholder = { Text("Tin nhắn", fontSize = 15.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .heightIn(max = 120.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedBorderColor = Color.Transparent
                                ),
                                singleLine = false
                            )
                            
                            if (input.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        viewModel.sendMessage(otherUid, input)
                                        input = ""
                                    },
                                    enabled = !state.isSending
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, "Gửi", tint = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                IconButton(onClick = { viewModel.sendMessage(otherUid, "👍") }) {
                                    Icon(Icons.Filled.ThumbUp, "Like", tint = MaterialTheme.colorScheme.primary) 
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface),
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
        ) {
            itemsIndexed(state.messages, key = { _, msg -> msg.id }) { index, message ->
                val isOwn = message.sender_id == viewModel.currentUid
                val showAvatar = !isOwn && (index == 0 || state.messages[index-1].sender_id != message.sender_id)
                val isLastInBlock = index == state.messages.size - 1 || state.messages[index+1].sender_id != message.sender_id
                
                MessageBubble(
                    message = message,
                    isOwn = isOwn,
                    showAvatar = showAvatar,
                    isLastInBlock = isLastInBlock,
                    otherName = otherName
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message, 
    isOwn: Boolean, 
    showAvatar: Boolean, 
    isLastInBlock: Boolean,
    otherName: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isLastInBlock) 4.dp else 1.dp),
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
                        Text(
                            text = otherName.take(1).uppercase(), 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold
                        )
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
                bottomStart = if (isOwn) 18.dp else if (isLastInBlock) 4.dp else 18.dp,
                bottomEnd = if (isOwn) (if (isLastInBlock) 4.dp else 18.dp) else 18.dp
            ),
            color = if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (message.content.startsWith("Sent an image:")) {
                    val imageUrl = message.content.substringAfterLast("\n").trim()
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = message.content,
                        color = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
