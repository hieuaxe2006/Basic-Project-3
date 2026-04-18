package com.socialapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.socialapp.data.model.User
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val currentUid get() = auth.currentUser?.uid

    suspend fun getUser(uid: String): Result<User> = runCatching {
        val doc = db.collection("users").document(uid).get().await()
        doc.toObject(User::class.java)?.copy(id = doc.id) ?: throw Exception("User not found")
    }

    suspend fun updateProfile(uid: String, username: String, bio: String, avatar: String?): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "username" to username,
            "bio" to bio
        )
        avatar?.let { updates["avatar"] = it }
        db.collection("users").document(uid).update(updates).await()
    }
}
