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

    suspend fun createPost(content: String, imageBase64: String?): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")

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
            created_at = Timestamp.now()
        )
        docRef.set(post).await()
    }
}
