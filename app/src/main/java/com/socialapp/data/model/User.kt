package com.socialapp.data.model

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val avatar: String = "",
    val bio: String = "",
    val note: String = "",
    val fcm_token: String = "", // Thêm biến này
    val followers_count: Int = 0,
    val following_count: Int = 0,
    val is_premium: Boolean = false,
    val is_private: Boolean = false,
    val is_blocked: Boolean = false,
    val role: String = "user",
    val height: Double = 0.0,
    val weight: Double = 0.0,
    val body_fat: Double = 0.0,
    val bench_pr: Double = 0.0,
    val squat_pr: Double = 0.0,
    val deadlift_pr: Double = 0.0
)