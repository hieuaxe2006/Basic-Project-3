package com.socialapp.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color // THÊM IMPORT NÀY
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // THÊM IMPORT NÀY
import coil.compose.AsyncImage
import com.socialapp.data.model.Post
import com.socialapp.data.model.User
import com.google.firebase.Timestamp
import com.socialapp.ui.post.CodeBlock // Đảm bảo đúng package
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun PostItem(
    post: Post,
    user: User?,
    isLiked: Boolean,
    isSaved: Boolean = false,
    isFollowing: Boolean = false,
    isOwnPost: Boolean = false,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onUserClick: (String) -> Unit,
    onShare: () -> Unit = {},
    onSave: () -> Unit = {},
    onFollow: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onUserClick(post.user_id) }) {
                if (user?.avatar?.isNotBlank() == true) {
                    AsyncImage(model = user.avatar, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                } else {
                    Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(contentAlignment = Alignment.Center) { Text(user?.username?.take(1) ?: "?") }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user?.username ?: "Unknown", fontWeight = FontWeight.Bold)
                    Text(formatRelativeTime(post.created_at), fontSize = 12.sp, color = Color.Gray)
                }
            }

            if (post.content.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(post.content)
            }

            // HIỂN THỊ CODE
            if (post.code_snippet.isNotBlank()) {
                CodeBlock(code = post.code_snippet, language = post.language)
            }

            if (post.image_url.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                AsyncImage(model = post.image_url, contentDescription = null, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)))
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = onLike) { Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isLiked) Color.Red else Color.Gray) }
                Text("${post.like_count}", fontSize = 13.sp)
                IconButton(onClick = onComment) { Icon(Icons.Outlined.ChatBubbleOutline, null) }
                Text("${post.comment_count}", fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSave) { Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null, tint = if (isSaved) Color.Blue else Color.Gray) }
            }
        }
    }
}

// HÀM NÀY PHẢI NẰM NGOÀI CÙNG FILE
fun formatRelativeTime(timestamp: Timestamp): String {
    val diff = Date().time - timestamp.toDate().time
    return when {
        diff < 60000 -> "Vừa xong"
        diff < 3600000 -> "${diff / 60000} phút trước"
        diff < 86400000 -> "${diff / 3600000} giờ trước"
        else -> "${diff / 86400000} ngày trước"
    }
}