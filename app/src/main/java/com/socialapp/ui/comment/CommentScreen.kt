package com.socialapp.ui.comment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImage
import com.socialapp.data.model.Comment
import com.socialapp.data.model.User
import com.socialapp.ui.home.PostItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    postId: String,
    viewModel: CommentViewModel,
    onBack: () -> Unit
) {
    var input by remember { mutableStateOf("") }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Write a comment...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            viewModel.addComment(postId, input)
                            input = ""
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
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Top section for Post
            if (state.post != null) {
                PostItem(
                    post = state.post,
                    user = state.postUser,
                    isLiked = false, // mock
                    onLike = { },
                    onComment = { },
                    onUserClick = { }
                )
            } else if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Post not found")
                }
            }
            HorizontalDivider()
            
            // Bottom section for Comments
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.comments.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No comments yet", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.comments, key = { it.id }) { comment ->
                                CommentItem(comment, state.userMap[comment.user_id])
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: Comment, user: User?) {
    Row(modifier = Modifier.fillMaxWidth()) {
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
        Column {
            Text(user?.username ?: "Unknown", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(comment.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
