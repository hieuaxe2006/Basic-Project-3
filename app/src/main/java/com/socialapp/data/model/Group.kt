package com.socialapp.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val avatarUrl: String = "",
    val memberCount: Int = 0,
    val postCount: Int = 0,
    val memberIds: List<String> = emptyList()
)
