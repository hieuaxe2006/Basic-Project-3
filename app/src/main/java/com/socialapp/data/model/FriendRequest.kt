package com.socialapp.data.model

import com.google.firebase.Timestamp

data class FriendRequest(
    val id: String = "",
    val sender_id: String = "",
    val receiver_id: String = "",
    val status: String = "pending", // pending, accepted, declined
    val created_at: Timestamp = Timestamp.now()
)
