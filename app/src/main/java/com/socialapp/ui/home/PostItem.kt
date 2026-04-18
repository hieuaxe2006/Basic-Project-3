package com.socialapp.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.socialapp.data.model.Post
import com.socialapp.data.model.User

@Composable
fun PostItem(
    post: Post,
    user: User?,
    isLiked: Boolean,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onUserClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onUserClick(post.user_id) }
            ) {
                if (user?.avatar?.isNotBlank() == true) {
                    AsyncImage(
                        model = user.avatar,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                user?.username?.take(1)?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(user?.username ?: "Unknown", fontWeight = FontWeight.SemiBold)
                
                Spacer(Modifier.width(8.dp))
                var following by remember { mutableStateOf(false) }
                // Real app should verify if this is NOT own post, but mock for now
                Text(
                    text = if (following) "Following" else "Follow",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clickable { following = !following }.padding(4.dp)
                )
            }

            if (post.content.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(post.content, style = MaterialTheme.typography.bodyLarge)
            }

            if (post.image_url.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = post.image_url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onLike) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text("${post.like_count}", style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.width(16.dp))

                IconButton(onClick = onComment) {
                    Icon(Icons.Default.Email, contentDescription = "Comment")
                }
                Text("${post.comment_count}", style = MaterialTheme.typography.bodySmall)
                
                Spacer(Modifier.width(16.dp))
                
                IconButton(onClick = { /* Mock view logic over 3 seconds */ }) {
                    Icon(Icons.Default.Visibility, contentDescription = "Views", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("0", style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.weight(1f))

                var isSaved by remember { mutableStateOf(false) }
                IconButton(onClick = { isSaved = !isSaved }) {
                    Icon(
                        if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, 
                        contentDescription = "Save",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
