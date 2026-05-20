package com.socialapp.data.model

import com.google.firebase.Timestamp

data class Story(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val text: String = "",
    val backgroundColor: String = "",
    val type: String = "image", // "image", "video", "text"
    val visibility: String = "public", // "public", "friends", "private"
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp.now()
)
