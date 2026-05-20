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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import com.socialapp.ui.post.WorkoutLogBlock // Đảm bảo đúng package
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
    onFollow: () -> Unit = {},
    onUpdateVisibility: (Boolean) -> Unit = {},
    onUpdateCommentsDisabled: (Boolean) -> Unit = {},
    onDeletePost: () -> Unit = {}
) {
    var showUnfollowDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (showUnfollowDialog) {
        AlertDialog(
            onDismissRequest = { showUnfollowDialog = false },
            title = { Text("Hủy theo dõi") },
            text = { Text("Bạn có chắc chắn muốn hủy theo dõi ${user?.username ?: "người dùng này"}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnfollowDialog = false
                        onFollow()
                    }
                ) {
                    Text("Đồng ý", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnfollowDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).clickable { onUserClick(post.user_id) }
                ) {
                    if (user?.avatar?.isNotBlank() == true) {
                        AsyncImage(model = user.avatar, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Box(contentAlignment = Alignment.Center) { Text(user?.username?.take(1) ?: "?") }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user?.username ?: "Unknown", fontWeight = FontWeight.Bold)
                            if (!isOwnPost) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isFollowing) "• Đang theo dõi" else "• Theo dõi",
                                    color = if (isFollowing) Color.Gray else MaterialTheme.colorScheme.primary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        if (isFollowing) {
                                            showUnfollowDialog = true
                                        } else {
                                            onFollow()
                                        }
                                    }
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatRelativeTime(post.created_at), fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (post.is_private) "• Riêng tư" else "• Công khai",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Settings", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (isOwnPost) {
                            DropdownMenuItem(
                                text = { Text(if (post.is_private) "Chuyển sang Công khai" else "Chuyển sang Riêng tư") },
                                onClick = {
                                    showMenu = false
                                    onUpdateVisibility(!post.is_private)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (post.comments_disabled) "Cho phép bình luận" else "Chặn bình luận") },
                                onClick = {
                                    showMenu = false
                                    onUpdateCommentsDisabled(!post.comments_disabled)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa bài viết", color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    onDeletePost()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Báo cáo bài viết") },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Ẩn bài viết") },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                }
            }

            if (post.content.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(post.content)
            }

            // HIỂN THỊ WORKOUT LOG
            if (post.code_snippet.isNotBlank()) {
                WorkoutLogBlock(log = post.code_snippet, workoutType = post.language)
            }

            if (post.image_url.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                AsyncImage(model = post.image_url, contentDescription = null, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)))
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                IconButton(onClick = onLike) { Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isLiked) Color.Red else Color.Gray) }
                Text("${post.like_count}", fontSize = 13.sp)
                if (!post.comments_disabled) {
                    IconButton(onClick = onComment) { Icon(Icons.Outlined.ChatBubbleOutline, null) }
                    Text("${post.comment_count}", fontSize = 13.sp)
                } else {
                    Spacer(Modifier.width(16.dp))
                    Text("Bình luận đã tắt", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterVertically))
                }
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