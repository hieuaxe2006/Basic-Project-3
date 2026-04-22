package com.socialapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.socialapp.data.model.Comment
import com.socialapp.data.model.Post
import com.socialapp.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SocialRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    val currentUid get() = auth.currentUser?.uid

    // ── Follow ──

    suspend fun toggleFollow(targetUid: String): Result<Boolean> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        if (uid == targetUid) throw Exception("Cannot follow yourself")

        val followQuery = db.collection("follows")
            .whereEqualTo("follower_id", uid)
            .whereEqualTo("following_id", targetUid)
            .get().await()

        if (followQuery.isEmpty) {
            val docRef = db.collection("follows").document()
            docRef.set(mapOf("id" to docRef.id, "follower_id" to uid, "following_id" to targetUid)).await()
            db.collection("users").document(uid).update("following_count", FieldValue.increment(1)).await()
            db.collection("users").document(targetUid).update("followers_count", FieldValue.increment(1)).await()
            true
        } else {
            followQuery.documents.first().reference.delete().await()
            db.collection("users").document(uid).update("following_count", FieldValue.increment(-1)).await()
            db.collection("users").document(targetUid).update("followers_count", FieldValue.increment(-1)).await()
            false
        }
    }

    suspend fun isFollowing(targetUid: String): Boolean {
        val uid = currentUid ?: return false
        val query = db.collection("follows")
            .whereEqualTo("follower_id", uid)
            .whereEqualTo("following_id", targetUid)
            .get().await()
        return !query.isEmpty
    }

    fun getFollowingIdsFlow(): Flow<Set<String>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            trySend(emptySet())
            close()
            return@callbackFlow
        }
        val listener = db.collection("follows")
            .whereEqualTo("follower_id", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents?.mapNotNull { it.getString("following_id") }?.toSet() ?: emptySet()
                trySend(ids)
            }
        awaitClose { listener.remove() }
    }

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

    // ── Save/Bookmark ──

    suspend fun toggleSavePost(postId: String): Result<Boolean> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val saveQuery = db.collection("saved_posts")
            .whereEqualTo("user_id", uid)
            .whereEqualTo("post_id", postId)
            .get().await()

        if (saveQuery.isEmpty) {
            val docRef = db.collection("saved_posts").document()
            docRef.set(mapOf("id" to docRef.id, "user_id" to uid, "post_id" to postId, "saved_at" to Timestamp.now())).await()
            true
        } else {
            saveQuery.documents.first().reference.delete().await()
            false
        }
    }

    fun getSavedPostIdsFlow(): Flow<Set<String>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            trySend(emptySet())
            close()
            return@callbackFlow
        }
        val listener = db.collection("saved_posts")
            .whereEqualTo("user_id", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val ids = snapshot?.documents?.mapNotNull { it.getString("post_id") }?.toSet() ?: emptySet()
                trySend(ids)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getSavedPosts(): Result<List<Post>> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val savedQuery = db.collection("saved_posts")
            .whereEqualTo("user_id", uid)
            .get().await()

        val postIds = savedQuery.documents.mapNotNull { it.getString("post_id") }
        if (postIds.isEmpty()) return@runCatching emptyList()

        val result = mutableListOf<Post>()
        postIds.chunked(10).forEach { chunk ->
            val postsQuery = db.collection("posts").whereIn("id", chunk).get().await()
            result.addAll(postsQuery.toObjects(Post::class.java))
        }
        result.sortedByDescending { it.created_at }
    }

    // ── Comment ──

    suspend fun addComment(postId: String, content: String, parentId: String = ""): Result<Comment> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val docRef = db.collection("comments").document()
        val comment = Comment(
            id = docRef.id,
            post_id = postId,
            user_id = uid,
            content = content,
            created_at = Timestamp.now(),
            parent_id = parentId
        )
        docRef.set(comment).await()
        if (parentId.isBlank()) {
            db.collection("posts").document(postId).update("comment_count", FieldValue.increment(1)).await()
        }
        comment
    }

    suspend fun toggleCommentLike(commentId: String): Result<Boolean> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val likeQuery = db.collection("comment_likes")
            .whereEqualTo("user_id", uid)
            .whereEqualTo("comment_id", commentId)
            .get().await()

        if (likeQuery.isEmpty) {
            val docRef = db.collection("comment_likes").document()
            docRef.set(mapOf("id" to docRef.id, "user_id" to uid, "comment_id" to commentId)).await()
            db.collection("comments").document(commentId).update("like_count", FieldValue.increment(1)).await()
            true
        } else {
            likeQuery.documents.first().reference.delete().await()
            db.collection("comments").document(commentId).update("like_count", FieldValue.increment(-1)).await()
            false
        }
    }

    suspend fun getComments(postId: String): Result<List<Comment>> = runCatching {
        db.collection("comments")
            .whereEqualTo("post_id", postId)
            .get().await()
            .toObjects(Comment::class.java)
            .sortedByDescending { it.created_at }
    }

    suspend fun getLikedCommentIds(commentIds: List<String>): Set<String> {
        val uid = currentUid ?: return emptySet()
        if (commentIds.isEmpty()) return emptySet()
        val result = mutableSetOf<String>()
        commentIds.chunked(10).forEach { chunk ->
            val query = db.collection("comment_likes")
                .whereEqualTo("user_id", uid)
                .whereIn("comment_id", chunk)
                .get().await()
            query.documents.forEach { doc ->
                doc.getString("comment_id")?.let { result.add(it) }
            }
        }
        return result
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

    suspend fun getUserPosts(uid: String): Result<List<Post>> = runCatching {
        db.collection("posts")
            .whereEqualTo("user_id", uid)
            .get().await()
            .toObjects(Post::class.java)
            .sortedByDescending { it.created_at }
    }

    suspend fun getUser(uid: String): User? {
        return db.collection("users").document(uid).get().await().toObject(User::class.java)
    }

    // ── Search & Recommendations ──

    suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        db.collection("users")
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThanOrEqualTo("username", query + "\uf8ff")
            .get().await()
            .toObjects(User::class.java)
    }

    suspend fun getRecommendedUsers(): Result<List<User>> = runCatching {
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

    suspend fun getDomainCounts(): Result<Map<String, Int>> = runCatching {
        val posts = db.collection("posts")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(200)
            .get().await()
            .toObjects(Post::class.java)

        val counts = mutableMapOf<String, Int>()
        posts.forEach { post ->
            post.tags.forEach { tag ->
                counts[tag] = counts.getOrDefault(tag, 0) + 1
            }
        }
        counts
    }

    // ── Settings ──

    suspend fun updatePremiumStatus(isPremium: Boolean): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        db.collection("users").document(uid).update("is_premium", isPremium).await()
    }

    suspend fun updatePrivateStatus(isPrivate: Boolean): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        db.collection("users").document(uid).update("is_private", isPrivate).await()
    }
}
