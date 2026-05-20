package com.socialapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.socialapp.data.model.Post
import com.socialapp.data.model.User
import com.socialapp.data.model.Notification
import kotlinx.coroutines.tasks.await

class AdminRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getAllUsers(): List<User> =
        db.collection("users").get().await().toObjects(User::class.java)

    suspend fun getAllPosts(): List<Post> =
        db.collection("posts").orderBy("created_at", Query.Direction.DESCENDING).get().await().toObjects(Post::class.java)

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        db.collection("posts").document(postId).delete().await()
    }

    suspend fun approvePost(postId: String): Result<Unit> = runCatching {
        val postRef = db.collection("posts").document(postId)
        postRef.update("status", "approved", "moderated_at", Timestamp.now()).await()

        val postDoc = postRef.get().await()
        val post = postDoc.toObject(Post::class.java)
        if (post != null) {
            val notification = Notification(
                receiverId = post.user_id,
                senderId = "system",
                senderName = "Hệ thống",
                senderAvatar = "",
                type = "post_approved",
                postId = postId,
                content = "Bài viết của bạn đã được phê duyệt và hiển thị với mọi người!"
            )
            val notifRef = db.collection("notifications").document()
            notifRef.set(notification.copy(id = notifRef.id, createdAt = Timestamp.now())).await()
        }
    }

    suspend fun rejectPost(postId: String): Result<Unit> = runCatching {
        val postRef = db.collection("posts").document(postId)
        postRef.update("status", "rejected", "moderated_at", Timestamp.now()).await()

        val postDoc = postRef.get().await()
        val post = postDoc.toObject(Post::class.java)
        if (post != null) {
            val notification = Notification(
                receiverId = post.user_id,
                senderId = "system",
                senderName = "Hệ thống",
                senderAvatar = "",
                type = "post_rejected",
                postId = postId,
                content = "Bài viết của bạn đã bị từ chối phê duyệt do vi phạm tiêu chuẩn cộng đồng."
            )
            val notifRef = db.collection("notifications").document()
            notifRef.set(notification.copy(id = notifRef.id, createdAt = Timestamp.now())).await()
        }
    }

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