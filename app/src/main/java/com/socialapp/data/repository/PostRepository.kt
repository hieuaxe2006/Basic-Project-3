package com.socialapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.socialapp.data.model.Post
import com.socialapp.data.remote.ImgBBApi
import kotlinx.coroutines.tasks.await

class PostRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val currentUid get() = auth.currentUser?.uid

    suspend fun createPost(
        content: String,
        imageBase64: String?,
        tags: List<String> = emptyList(),
        backgroundColor: String = "",
        codeSnippet: String = "",
        language: String = "",
        taggedUserIds: List<String> = emptyList()
    ): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")

        // Kiểm tra quyền Premium và giới hạn bài đăng
        val userDoc = db.collection("users").document(uid).get().await()
        val isPremium = userDoc.getBoolean("is_premium") ?: false

        if (!isPremium) {
            val postsQuery = db.collection("posts")
                .whereEqualTo("user_id", uid)
                .get()
                .await()

            // Giới hạn 5 bài cho tài khoản thường
            if (postsQuery.size() >= 5) {
                throw Exception("Bạn đã đạt giới hạn 5 bài đăng cho tài khoản miễn phí. Vui lòng nâng cấp Premium để đăng bài không giới hạn!")
            }
        }

        var imageUrl = ""
        if (!imageBase64.isNullOrBlank()) {
            imageUrl = ImgBBApi.uploadImage(imageBase64).getOrThrow()
        }

        val docRef = db.collection("posts").document()
        val post = Post(
            id = docRef.id,
            user_id = uid,
            content = content,
            image_url = imageUrl,
            code_snippet = codeSnippet,
            language = language,
            tagged_user_ids = taggedUserIds,
            created_at = Timestamp.now(),
            tags = tags,
            background_color = backgroundColor,
            status = "pending"
        )
        docRef.set(post).await()
    }
}