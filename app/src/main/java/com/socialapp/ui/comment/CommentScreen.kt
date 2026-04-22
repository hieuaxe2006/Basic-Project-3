package com.socialapp.ui.comment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.socialapp.data.model.Comment
import com.socialapp.data.model.User

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
                title = { Text("Comments") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    if (replyToId.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Replying to @$replyToUsername",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                replyToId = ""
                                replyToUsername = ""
                            }) { Text("Cancel") }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text(if (replyToId.isNotBlank()) "Reply..." else "Write a comment...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                viewModel.addComment(postId, input, replyToId)
                                input = ""
                                replyToId = ""
                                replyToUsername = ""
                            },
                            enabled = input.isNotBlank() && !state.isSending
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
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Top 40% - Post thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
            ) {
                if (state.post != null) {
                    CompactPostView(post = state.post, user = state.postUser)
                } else if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.error ?: "Post not found")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.loadComments(postId) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Bottom 60% - Comments
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.comments.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (state.error != null) state.error!! else "No comments yet")
                                if (state.error != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { viewModel.loadComments(postId) }) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // Separate top-level comments and replies
                        val topLevel = state.comments.filter { it.parent_id.isBlank() }
                        val replies = state.comments.filter { it.parent_id.isNotBlank() }
                        val replyMap = replies.groupBy { it.parent_id }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(topLevel, key = { it.id }) { comment ->
                                CommentItem(
                                    comment = comment,
                                    user = state.userMap[comment.user_id],
                                    isLiked = comment.id in state.likedCommentIds,
                                    onLike = { viewModel.toggleCommentLike(comment.id) },
                                    onReply = {
                                        replyToId = comment.id
                                        replyToUsername = state.userMap[comment.user_id]?.username ?: "user"
                                    }
                                )
                                // Show replies
                                replyMap[comment.id]?.forEach { reply ->
                                    Row(modifier = Modifier.padding(start = 32.dp, top = 8.dp)) {
                                        CommentItem(
                                            comment = reply,
                                            user = state.userMap[reply.user_id],
                                            isLiked = reply.id in state.likedCommentIds,
                                            onLike = { viewModel.toggleCommentLike(reply.id) },
                                            onReply = {
                                                replyToId = comment.id
                                                replyToUsername = state.userMap[reply.user_id]?.username ?: "user"
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPostView(
    post: com.socialapp.data.model.Post,
    user: User?
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (user?.avatar?.isNotBlank() == true) {
                    AsyncImage(
                        model = user.avatar,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(user?.username?.take(1)?.uppercase() ?: "?", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(user?.username ?: "Unknown", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            }

            if (post.content.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(post.content, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }

            if (post.image_url.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = post.image_url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(4.dp))
            Row {
                Text("❤️ ${post.like_count}", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(12.dp))
                Text("💬 ${post.comment_count}", style = MaterialTheme.typography.labelSmall)
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
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        if (user?.avatar?.isNotBlank() == true) {
            AsyncImage(
                model = user.avatar,
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(user?.username?.take(1)?.uppercase() ?: "?", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user?.username ?: "Unknown", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(comment.content, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLike, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        modifier = Modifier.size(14.dp),
                        tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("${comment.like_count}", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(12.dp))
                IconButton(onClick = onReply, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Reply",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("Reply", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
