package com.socialapp.data.model

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val user_id: String = "",
    val content: String = "",
    val image_urls: List<String> = emptyList(), // Số nhiều
    val code_snippet: String = "",
    val language: String = "",
    val tagged_user_ids: List<String> = emptyList(),
    val created_at: Timestamp = Timestamp.now(),
    val like_count: Int = 0,
    val comment_count: Int = 0,
    val tags: List<String> = emptyList(),
    val background_color: String = "",
    val is_private: Boolean = false,
    val comments_disabled: Boolean = false,
    val group_id: String = "",
    val status: String = "pending",
    val moderated_at: Timestamp? = null
)