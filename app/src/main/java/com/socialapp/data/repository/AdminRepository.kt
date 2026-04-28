package com.socialapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.socialapp.data.model.Post
import com.socialapp.data.model.User
import kotlinx.coroutines.tasks.await

class AdminRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getAllUsers(): List<User> =
        db.collection("users").get().await().toObjects(User::class.java)

    suspend fun getAllPosts(): List<Post> =
        db.collection("posts").orderBy("created_at", Query.Direction.DESCENDING).get().await().toObjects(Post::class.java)

    // Hàm xóa bài viết
    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        db.collection("posts").document(postId).delete().await()
    }

    // Hàm chặn hoặc bỏ chặn người dùng
    suspend fun toggleBlockUser(uid: String, isBlocked: Boolean): Result<Unit> = runCatching {
        db.collection("users").document(uid).update("is_blocked", isBlocked).await()
    }

    suspend fun getRevenueStats(): Long {
        val premiumCount = db.collection("users").whereEqualTo("is_premium", true).get().await().size()
        return premiumCount * 200000L
    }

    suspend fun getTopFollowers(): List<User> =
        db.collection("users").orderBy("followers_count", Query.Direction.DESCENDING).limit(5).get().await().toObjects(User::class.java)
}