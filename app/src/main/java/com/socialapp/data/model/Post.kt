package com.socialapp.data.model

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val user_id: String = "",
    val content: String = "",
    val image_url: String = "",
    val created_at: Timestamp = Timestamp.now(),
    val like_count: Int = 0,
    val comment_count: Int = 0,
    val tags: List<String> = emptyList(),
    val background_color: String = ""
)
