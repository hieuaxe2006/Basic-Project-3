package com.socialapp.ui.comment

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.socialapp.data.model.Comment
import com.socialapp.data.model.User
import com.socialapp.ui.home.formatRelativeTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    postId: String,
    viewModel: CommentViewModel,
    onBack: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var replyToId by remember { mutableStateOf("") }
    var replyToUsername by remember { mutableStateOf("") }
    val state = viewModel.state

    LaunchedEffect(postId) { viewModel.loadComments(postId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bình luận", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (replyToId.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Đang trả lời @$replyToUsername",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                replyToId = ""
                                replyToUsername = ""
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            TextField(
                                value = input,
                                onValueChange = { input = it },
                                placeholder = { Text("Viết bình luận...", fontSize = 14.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                maxLines = 4,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                viewModel.addComment(postId, input, replyToId)
                                input = ""
                                replyToId = ""
                                replyToUsername = ""
                            },
                            enabled = input.isNotBlank() && !state.isSending,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (state.isSending) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, "Send")
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Post Summary Header
            item {
                if (state.post != null) {
                    PostHeaderView(post = state.post, user = state.postUser)
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            // Comments List
            if (state.isLoading && state.comments.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.comments.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Chưa có bình luận nào. Hãy là người đầu tiên!", color = Color.Gray)
                    }
                }
            } else {
                val topLevel = state.comments.filter { it.parent_id.isBlank() }
                val replyMap = state.comments.filter { it.parent_id.isNotBlank() }.groupBy { it.parent_id }

                items(topLevel, key = { it.id }) { comment ->
                    CommentItem(
                        comment = comment,
                        user = state.userMap[comment.user_id],
                        isLiked = comment.id in state.likedCommentIds,
                        onLike = { viewModel.toggleCommentLike(comment.id) },
                        onReply = {
                            replyToId = comment.id
                            replyToUsername = state.userMap[comment.user_id]?.username ?: "người dùng"
                        }
                    )
                    
                    // Display replies
                    replyMap[comment.id]?.forEach { reply ->
                        CommentItem(
                            comment = reply,
                            user = state.userMap[reply.user_id],
                            isLiked = reply.id in state.likedCommentIds,
                            onLike = { viewModel.toggleCommentLike(reply.id) },
                            onReply = {
                                replyToId = comment.id
                                replyToUsername = state.userMap[reply.user_id]?.username ?: "người dùng"
                            },
                            isReply = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostHeaderView(
    post: com.socialapp.data.model.Post,
    user: User?
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (user?.avatar?.isNotBlank() == true) {
                    AsyncImage(
                        model = user.avatar,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(user?.username?.take(1)?.uppercase() ?: "?", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(user?.username ?: "Người dùng", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(formatRelativeTime(post.created_at), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            if (post.content.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(post.content, style = MaterialTheme.typography.bodyLarge)
            }

            if (post.image_url.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = post.image_url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    user: User?,
    isLiked: Boolean,
    onLike: () -> Unit,
    onReply: () -> Unit,
    isReply: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = if (isReply) 56.dp else 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        if (user?.avatar?.isNotBlank() == true) {
            AsyncImage(
                model = user.avatar,
                contentDescription = null,
                modifier = Modifier.size(if (isReply) 28.dp else 36.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(modifier = Modifier.size(if (isReply) 28.dp else 36.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Text(user?.username?.take(1)?.uppercase() ?: "?", fontSize = if (isReply) 11.sp else 14.sp)
                }
            }
        }
        
        Spacer(Modifier.width(8.dp))
        
        Column {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(user?.username ?: "Người dùng", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(comment.content, style = MaterialTheme.typography.bodyMedium, fontSize = 14.sp)
                }
            }
            
            Row(
                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Thích",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isLiked) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clickable { onLike() }
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "Phản hồi",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.clickable { onReply() }
                )
                
                if (comment.like_count > 0) {
                    Spacer(Modifier.width(16.dp))
                    Icon(Icons.Default.Favorite, null, tint = Color(0xFFE41E3F), modifier = Modifier.size(10.dp))
                    Text(" ${comment.like_count}", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}
