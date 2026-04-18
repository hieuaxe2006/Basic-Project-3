package com.socialapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.socialapp.data.model.Comment
import com.socialapp.data.model.Post
import com.socialapp.data.model.User
import kotlinx.coroutines.tasks.await

class SocialRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    val currentUid get() = auth.currentUser?.uid

    // ── Like ──

    suspend fun toggleLike(postId: String): Result<Boolean> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val likeQuery = db.collection("likes")
            .whereEqualTo("user_id", uid)
            .whereEqualTo("post_id", postId)
            .get().await()

        if (likeQuery.isEmpty) {
            val docRef = db.collection("likes").document()
            docRef.set(mapOf("id" to docRef.id, "user_id" to uid, "post_id" to postId)).await()
            db.collection("posts").document(postId).update("like_count", FieldValue.increment(1)).await()
            true
        } else {
            likeQuery.documents.first().reference.delete().await()
            db.collection("posts").document(postId).update("like_count", FieldValue.increment(-1)).await()
            false
        }
    }

    suspend fun isLiked(postId: String): Boolean {
        val uid = currentUid ?: return false
        val query = db.collection("likes")
            .whereEqualTo("user_id", uid)
            .whereEqualTo("post_id", postId)
            .get().await()
        return !query.isEmpty
    }

    suspend fun getLikedPostIds(postIds: List<String>): Set<String> {
        val uid = currentUid ?: return emptySet()
        if (postIds.isEmpty()) return emptySet()
        val result = mutableSetOf<String>()
        postIds.chunked(10).forEach { chunk ->
            val query = db.collection("likes")
                .whereEqualTo("user_id", uid)
                .whereIn("post_id", chunk)
                .get().await()
            query.documents.forEach { doc ->
                doc.getString("post_id")?.let { result.add(it) }
            }
        }
        return result
    }

    // ── Comment ──

    suspend fun addComment(postId: String, content: String): Result<Comment> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val docRef = db.collection("comments").document()
        val comment = Comment(
            id = docRef.id,
            post_id = postId,
            user_id = uid,
            content = content,
            created_at = Timestamp.now()
        )
        docRef.set(comment).await()
        db.collection("posts").document(postId).update("comment_count", FieldValue.increment(1)).await()
        comment
    }

    suspend fun getComments(postId: String): Result<List<Comment>> = runCatching {
        db.collection("comments")
            .whereEqualTo("post_id", postId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(Comment::class.java)
    }

    // ── Feed ──

    suspend fun getFeed(limit: Long = 20): Result<List<Post>> = runCatching {
        db.collection("posts")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(limit)
            .get().await()
            .toObjects(Post::class.java)
    }

    suspend fun getPost(postId: String): Result<Post> = runCatching {
        db.collection("posts").document(postId).get().await().toObject(Post::class.java)
            ?: throw Exception("Post not found")
    }

    suspend fun getUser(uid: String): User? {
        return db.collection("users").document(uid).get().await().toObject(User::class.java)
    }

    // ── Search & Recommendations ──
    
    suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        // Simple prefix search using >= and <= constraints on username
        db.collection("users")
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThanOrEqualTo("username", query + "\uf8ff")
            .get().await()
            .toObjects(User::class.java)
    }
    
    suspend fun getRecommendedUsers(): Result<List<User>> = runCatching {
        // Fetch users to recommend, limit for dashboard
        db.collection("users")
            .limit(5)
            .get().await()
            .toObjects(User::class.java)
            .filter { it.id != currentUid }
    }
    
    // ── Analytics & Explore ──
    
    suspend fun getHashtagCounts(): Result<Map<String, Int>> = runCatching {
        val posts = db.collection("posts")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(100)
            .get().await()
            .toObjects(Post::class.java)
            
        val counts = mutableMapOf<String, Int>()
        val hashtagRegex = Regex("""#\w+""")
        
        posts.forEach { post ->
            val tags = hashtagRegex.findAll(post.content).map { it.value }.toSet()
            tags.forEach { tag ->
                counts[tag] = counts.getOrDefault(tag, 0) + 1
            }
        }
        counts.toSortedMap()
    }
    
    // ── Settings ──
    
    suspend fun updatePremiumStatus(isPremium: Boolean): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        db.collection("users").document(uid).update("is_premium", isPremium).await()
    }
}
