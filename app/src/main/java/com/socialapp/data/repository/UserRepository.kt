package com.socialapp.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.socialapp.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val currentUid get() = auth.currentUser?.uid

    // Thêm hàm này để lưu Token FCM vào Firestore
    suspend fun updateFcmToken(token: String): Result<Unit> = runCatching {
        val uid = currentUid ?: return@runCatching
        db.collection("users").document(uid).update("fcm_token", token).await()
    }

    suspend fun getUser(uid: String): Result<User> = runCatching {
        val doc = db.collection("users").document(uid).get().await()
        doc.toObject(User::class.java)?.copy(id = doc.id) ?: throw Exception("User not found")
    }

    suspend fun updateProfile(uid: String, username: String, bio: String, avatar: String?): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>("username" to username, "bio" to bio)
        avatar?.let { updates["avatar"] = it }
        db.collection("users").document(uid).update(updates).await()
    }

    suspend fun updateProfileExt(
        uid: String,
        username: String,
        bio: String,
        age: Int,
        hometown: String,
        birthday: String,
        hobbies: String,
        trainingRegime: String
    ): Result<Unit> = runCatching {
        val updates = mapOf<String, Any>(
            "username" to username,
            "bio" to bio,
            "age" to age,
            "hometown" to hometown,
            "birthday" to birthday,
            "hobbies" to hobbies,
            "trainingRegime" to trainingRegime
        )
        db.collection("users").document(uid).update(updates).await()
    }

    suspend fun updateAvatar(uid: String, avatarUrl: String): Result<Unit> = runCatching {
        db.collection("users").document(uid).update("avatar", avatarUrl).await()
    }

    suspend fun updateGymMetrics(
        uid: String,
        height: Double,
        weight: Double,
        bodyFat: Double,
        benchPr: Double,
        squatPr: Double,
        deadliftPr: Double
    ): Result<Unit> = runCatching {
        val updates = mapOf<String, Any>(
            "height" to height,
            "weight" to weight,
            "body_fat" to bodyFat,
            "bench_pr" to benchPr,
            "squat_pr" to squatPr,
            "deadlift_pr" to deadliftPr
        )
        db.collection("users").document(uid).update(updates).await()
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw Exception("Not logged in")
        val email = user.email ?: throw Exception("No email found")
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential).await()
        user.updatePassword(newPassword).await()
    }

    fun getUserSnapshot(uid: String): Flow<User> = callbackFlow {
        val listener = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            snapshot?.toObject(User::class.java)?.copy(id = snapshot.id)?.let { trySend(it) }
        }
        awaitClose { listener.remove() }
    }

    suspend fun getFollowers(uid: String): Result<List<User>> = runCatching {
        val query = db.collection("follows").whereEqualTo("following_id", uid).get().await()
        val followerIds = query.documents.mapNotNull { it.getString("follower_id") }
        if (followerIds.isEmpty()) return@runCatching emptyList()
        val result = mutableListOf<User>()
        followerIds.chunked(10).forEach { chunk ->
            result.addAll(db.collection("users").whereIn("id", chunk).get().await().toObjects(User::class.java))
        }
        result
    }

    suspend fun getFollowing(uid: String): Result<List<User>> = runCatching {
        val query = db.collection("follows").whereEqualTo("follower_id", uid).get().await()
        val followingIds = query.documents.mapNotNull { it.getString("following_id") }
        if (followingIds.isEmpty()) return@runCatching emptyList()
        val result = mutableListOf<User>()
        followingIds.chunked(10).forEach { chunk ->
            result.addAll(db.collection("users").whereIn("id", chunk).get().await().toObjects(User::class.java))
        }
        result
    }
}