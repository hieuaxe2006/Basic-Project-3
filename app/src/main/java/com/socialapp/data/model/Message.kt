package com.socialapp.data.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val sender_id: String = "",
    val receiver_id: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
