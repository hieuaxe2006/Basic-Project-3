package com.socialapp.data.model

import com.google.firebase.Timestamp // THIẾU DÒNG NÀY SẼ GÂY RA 168 LỖI

data class Post(
    val id: String = "",
    val user_id: String = "",
    val content: String = "",
    val image_url: String = "",
    val code_snippet: String = "",
    val language: String = "",
    val tagged_user_ids: List<String> = emptyList(),
    val created_at: Timestamp = Timestamp.now(),
    val like_count: Int = 0,
    val comment_count: Int = 0,
    val tags: List<String> = emptyList(),
    val background_color: String = "",
    val is_private: Boolean = false,
    val comments_disabled: Boolean = false
)