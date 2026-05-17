package com.socialapp.data.model

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val receiverId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String = "",
    val type: String = "", // "like", "comment", "follow", "friend_request"
    val postId: String = "",
    val content: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val isSeen: Boolean = false
)
