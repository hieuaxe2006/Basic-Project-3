package com.socialapp.data.model

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val post_id: String = "",
    val user_id: String = "",
    val content: String = "",
    val created_at: Timestamp = Timestamp.now()
)
