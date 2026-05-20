package com.socialapp.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.socialapp.data.model.Comment
import com.socialapp.data.model.FriendRequest
import com.socialapp.data.model.Notification
import com.socialapp.data.model.Post
import com.socialapp.data.model.Story
import com.socialapp.data.model.User
import com.socialapp.data.model.Group
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class SocialRepository {
    internal val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    val currentUid get() = auth.currentUser?.uid

    // --- Story Logic ---
    suspend fun saveStory(story: Story): Result<Unit> = runCatching {
        val docRef = db.collection("stories").document()
        docRef.set(story.copy(id = docRef.id)).await()
    }

    suspend fun createStory(imageUrl: String): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val docRef = db.collection("stories").document()

        val calendar = Calendar.getInstance()
        val createdAt = Timestamp.now()
        calendar.time = createdAt.toDate()
        calendar.add(Calendar.DAY_OF_YEAR, 1) // Story expires in 24 hours
        val expiresAt = Timestamp(calendar.time)

        val story = Story(
            id = docRef.id,
            userId = uid,
            imageUrl = imageUrl,
            createdAt = createdAt,
            expiresAt = expiresAt,
            type = "image"
        )
        docRef.set(story).await()
    }

    suspend fun getStories(): Result<List<Story>> = runCatching {
        val now = Timestamp.now()
        db.collection("stories")
            .whereGreaterThan("expiresAt", now)
            .get()
            .await()
            .toObjects(Story::class.java)
            .sortedByDescending { it.createdAt }
    }

    suspend fun getStory(storyId: String): Story? {
        return db.collection("stories").document(storyId).get().await().toObject(Story::class.java)
    }

    // --- Friend Request & Follow Logic ---
    suspend fun sendFriendRequest(targetUid: String): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val currentUser = getUser(uid) ?: throw Exception("User not found")
        val existing = db.collection("friend_requests").whereEqualTo("sender_id", uid).whereEqualTo("receiver_id", targetUid).get().await()
        if (!existing.isEmpty) throw Exception("Request already sent")
        val docRef = db.collection("friend_requests").document()
        val request = FriendRequest(id = docRef.id, sender_id = uid, receiver_id = targetUid, status = "pending", created_at = Timestamp.now())
        docRef.set(request).await()

        createNotification(
            Notification(
                receiverId = targetUid,
                senderId = uid,
                senderName = currentUser.username,
                senderAvatar = currentUser.avatar,
                type = "friend_request",
                content = "${currentUser.username} đã gửi lời mời kết bạn."
            )
        )
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Unit> = runCatching {
        val requestDoc = db.collection("friend_requests").document(requestId).get().await()
        val request = requestDoc.toObject(FriendRequest::class.java) ?: throw Exception("Request not found")
        db.collection("friend_requests").document(requestId).update("status", "accepted").await()
        val friendDocId = if (request.sender_id < request.receiver_id) "${request.sender_id}_${request.receiver_id}" else "${request.receiver_id}_${request.sender_id}"
        db.collection("friends").document(friendDocId).set(mapOf("user1" to request.sender_id, "user2" to request.receiver_id, "since" to Timestamp.now(), "users" to listOf(request.sender_id, request.receiver_id))).await()

        val currentUser = getUser(request.receiver_id)
        if (currentUser != null) {
            createNotification(
                Notification(
                    receiverId = request.sender_id,
                    senderId = request.receiver_id,
                    senderName = currentUser.username,
                    senderAvatar = currentUser.avatar,
                    type = "friend_accept",
                    content = "${currentUser.username} đã chấp nhận lời mời kết bạn."
                )
            )
        }
    }

    suspend fun getFriendStatus(targetUid: String): String {
        val uid = currentUid ?: return "none"
        val friendDocId = if (uid < targetUid) "${uid}_${targetUid}" else "${targetUid}_${uid}"
        if (db.collection("friends").document(friendDocId).get().await().exists()) return "friends"
        if (!db.collection("friend_requests").whereEqualTo("sender_id", uid).whereEqualTo("receiver_id", targetUid).whereEqualTo("status", "pending").get().await().isEmpty) return "requested"
        if (!db.collection("friend_requests").whereEqualTo("sender_id", targetUid).whereEqualTo("receiver_id", uid).whereEqualTo("status", "pending").get().await().isEmpty) return "pending_approval"
        return "none"
    }

    suspend fun getFriends(): Result<List<User>> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val query = db.collection("friends").whereArrayContains("users", uid).get().await()
        val friendIds = query.documents.mapNotNull { doc -> (doc.get("users") as? List<String>)?.firstOrNull { it != uid } }
        if (friendIds.isEmpty()) return@runCatching emptyList()
        val users = mutableListOf<User>()
        friendIds.chunked(10).forEach { chunk -> users.addAll(db.collection("users").whereIn("id", chunk).get().await().toObjects(User::class.java)) }
        users
    }

    suspend fun getPendingRequests(): Result<List<Pair<FriendRequest, User>>> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val requests = db.collection("friend_requests").whereEqualTo("receiver_id", uid).whereEqualTo("status", "pending").get().await().toObjects(FriendRequest::class.java)
        requests.mapNotNull { req -> getUser(req.sender_id)?.let { req to it } }
    }

    // --- Block, Mute, Report ---
    suspend fun blockUser(targetUid: String): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        db.collection("users").document(uid).update("blocked_users", FieldValue.arrayUnion(targetUid)).await()
    }

    suspend fun unblockUser(targetUid: String): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        db.collection("users").document(uid).update("blocked_users", FieldValue.arrayRemove(targetUid)).await()
    }

    suspend fun isUserBlocked(targetUid: String): Boolean {
        val uid = currentUid ?: return false
        val user = getUser(uid) ?: return false
        return user.blocked_users.contains(targetUid)
    }

    suspend fun isBlockedBy(targetUid: String): Boolean {
        val uid = currentUid ?: return false
        val otherUser = getUser(targetUid) ?: return false
        return otherUser.blocked_users.contains(uid)
    }

    suspend fun muteUser(targetUid: String): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val user = getUser(uid) ?: throw Exception("User not found")
        val isMuted = user.muted_users.contains(targetUid)
        val update = if (isMuted) FieldValue.arrayRemove(targetUid) else FieldValue.arrayUnion(targetUid)
        db.collection("users").document(uid).update("muted_users", update).await()
    }

    suspend fun reportUser(targetUid: String, reason: String): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        db.collection("reports").add(mapOf(
            "reporter_id" to uid,
            "target_id" to targetUid,
            "reason" to reason,
            "timestamp" to Timestamp.now()
        )).await()
    }

    // --- Follow, Like, Save Flow ---
    suspend fun toggleFollow(targetUid: String): Result<Boolean> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val currentUser = getUser(uid) ?: throw Exception("User not found")
        val query = db.collection("follows").whereEqualTo("follower_id", uid).whereEqualTo("following_id", targetUid).get().await()
        if (query.isEmpty) {
            val targetUser = getUser(targetUid) ?: throw Exception("User not found")
            if (!targetUser.is_premium && targetUser.followers_count >= 10) {
                throw Exception("Gym gymer này đã đạt giới hạn 10 người theo dõi. Họ cần nâng cấp Premium!")
            }

            val docRef = db.collection("follows").document()
            docRef.set(mapOf("id" to docRef.id, "follower_id" to uid, "following_id" to targetUid)).await()
            db.collection("users").document(uid).update("following_count", FieldValue.increment(1)).await()
            db.collection("users").document(targetUid).update("followers_count", FieldValue.increment(1)).await()

            createNotification(
                Notification(
                    receiverId = targetUid,
                    senderId = uid,
                    senderName = currentUser.username,
                    senderAvatar = currentUser.avatar,
                    type = "follow",
                    content = "${currentUser.username} đã bắt đầu theo dõi bạn."
                )
            )
            true
        } else {
            query.documents.first().reference.delete().await()
            db.collection("users").document(uid).update("following_count", FieldValue.increment(-1)).await()
            db.collection("users").document(targetUid).update("followers_count", FieldValue.increment(-1)).await()
            false
        }
    }

    suspend fun isFollowing(targetUid: String): Boolean {
        val uid = currentUid ?: return false
        return !db.collection("follows").whereEqualTo("follower_id", uid).whereEqualTo("following_id", targetUid).get().await().isEmpty
    }

    fun getFollowingIdsFlow(): Flow<Set<String>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptySet()); close(); return@callbackFlow }
        val listener = db.collection("follows").whereEqualTo("follower_id", uid).addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.documents?.mapNotNull { it.getString("following_id") }?.toSet() ?: emptySet())
        }
        awaitClose { listener.remove() }
    }

    suspend fun toggleLike(postId: String): Result<Boolean> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val currentUser = getUser(uid) ?: throw Exception("User not found")
        val post = getPost(postId).getOrThrow()
        val query = db.collection("likes").whereEqualTo("user_id", uid).whereEqualTo("post_id", postId).get().await()
        if (query.isEmpty) {
            val docRef = db.collection("likes").document()
            docRef.set(mapOf("id" to docRef.id, "user_id" to uid, "post_id" to postId)).await()
            db.collection("posts").document(postId).update("like_count", FieldValue.increment(1)).await()

            if (post.user_id != uid) {
                createNotification(
                    Notification(
                        receiverId = post.user_id,
                        senderId = uid,
                        senderName = currentUser.username,
                        senderAvatar = currentUser.avatar,
                        type = "like",
                        postId = postId,
                        content = "${currentUser.username} đã thích bài viết của bạn."
                    )
                )
            }
            true
        } else {
            query.documents.first().reference.delete().await()
            db.collection("posts").document(postId).update("like_count", FieldValue.increment(-1)).await()
            false
        }
    }

    suspend fun getLikedPostIds(postIds: List<String>): Set<String> {
        val uid = currentUid ?: return emptySet()
        if (postIds.isEmpty()) return emptySet()
        val result = mutableSetOf<String>()
        postIds.chunked(10).forEach { chunk ->
            db.collection("likes").whereEqualTo("user_id", uid).whereIn("post_id", chunk).get().await().forEach { doc ->
                doc.getString("post_id")?.let { result.add(it) }
            }
        }
        return result
    }

    suspend fun toggleSavePost(postId: String): Result<Boolean> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val query = db.collection("saved_posts").whereEqualTo("user_id", uid).whereEqualTo("post_id", postId).get().await()
        if (query.isEmpty) {
            db.collection("saved_posts").document().set(mapOf("user_id" to uid, "post_id" to postId, "saved_at" to Timestamp.now())).await()
            true
        } else {
            query.documents.first().reference.delete().await()
            false
        }
    }

    fun getSavedPostIdsFlow(): Flow<Set<String>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run { trySend(emptySet()); close(); return@callbackFlow }
        val listener = db.collection("saved_posts").whereEqualTo("user_id", uid).addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.documents?.mapNotNull { it.getString("post_id") }?.toSet() ?: emptySet())
        }
        awaitClose { listener.remove() }
    }

    suspend fun getSavedPosts(): Result<List<Post>> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val postIds = db.collection("saved_posts").whereEqualTo("user_id", uid).get().await().mapNotNull { it.getString("post_id") }
        if (postIds.isEmpty()) return@runCatching emptyList()
        val posts = mutableListOf<Post>()
        postIds.chunked(10).forEach { chunk -> posts.addAll(db.collection("posts").whereIn("id", chunk).get().await().toObjects(Post::class.java)) }
        posts.sortedByDescending { it.created_at }
    }

    // --- Comment Logic ---
    suspend fun addComment(postId: String, content: String, parentId: String = ""): Result<Comment> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val currentUser = getUser(uid) ?: throw Exception("User not found")
        val post = getPost(postId).getOrThrow()
        val docRef = db.collection("comments").document()
        val comment = Comment(id = docRef.id, post_id = postId, user_id = uid, content = content, created_at = Timestamp.now(), parent_id = parentId)
        docRef.set(comment).await()
        if (parentId.isBlank()) db.collection("posts").document(postId).update("comment_count", FieldValue.increment(1)).await()

        if (post.user_id != uid) {
            createNotification(
                Notification(
                    receiverId = post.user_id,
                    senderId = uid,
                    senderName = currentUser.username,
                    senderAvatar = currentUser.avatar,
                    type = "comment",
                    postId = postId,
                    content = "${currentUser.username} đã bình luận về bài viết của bạn: $content"
                )
            )
        }
        comment
    }

    suspend fun getComments(postId: String): Result<List<Comment>> = runCatching {
        db.collection("comments").whereEqualTo("post_id", postId).get().await().toObjects(Comment::class.java).sortedByDescending { it.created_at }
    }

    suspend fun toggleCommentLike(commentId: String): Result<Boolean> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val query = db.collection("comment_likes").whereEqualTo("user_id", uid).whereEqualTo("comment_id", commentId).get().await()
        if (query.isEmpty) {
            db.collection("comment_likes").document().set(mapOf("user_id" to uid, "comment_id" to commentId)).await()
            db.collection("comments").document(commentId).update("like_count", FieldValue.increment(1)).await()
            true
        } else {
            query.documents.first().reference.delete().await()
            db.collection("comments").document(commentId).update("like_count", FieldValue.increment(-1)).await()
            false
        }
    }

    suspend fun getLikedCommentIds(commentIds: List<String>): Set<String> {
        val uid = currentUid ?: return emptySet()
        if (commentIds.isEmpty()) return emptySet()
        val result = mutableSetOf<String>()
        commentIds.chunked(10).forEach { chunk ->
            db.collection("comment_likes").whereEqualTo("user_id", uid).whereIn("comment_id", chunk).get().await().forEach { doc ->
                doc.getString("comment_id")?.let { result.add(it) }
            }
        }
        return result
    }

    // --- Notification Logic ---
    suspend fun createNotification(notification: Notification): Result<Unit> = runCatching {
        val docRef = db.collection("notifications").document()
        docRef.set(notification.copy(id = docRef.id, createdAt = Timestamp.now())).await()
    }

    fun getNotificationsFlow(): Flow<List<Notification>> = callbackFlow {
        var listener: ListenerRegistration? = null
        val auth = FirebaseAuth.getInstance()
        val uid = currentUid
        if (uid == null) {
            // Wait for auth state to provide UID
            val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                val newUid = firebaseAuth.currentUser?.uid
                if (newUid != null && listener == null) {
                    listener = db.collection("notifications")
                        .whereEqualTo("receiverId", newUid)
                        .limit(50)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.e("SocialRepository", "Notification listener error", error)
                                return@addSnapshotListener
                            }
                            snapshot?.let {
                                val notifications = it.toObjects(Notification::class.java)
                                    .sortedByDescending { it.createdAt }
                                trySend(notifications).isSuccess
                            }
                        }
                }
            }
            auth.addAuthStateListener(authListener)
            awaitClose {
                listener?.remove()
                auth.removeAuthStateListener(authListener)
            }
        } else {
            listener = db.collection("notifications")
                .whereEqualTo("receiverId", uid)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("SocialRepository", "Notification listener error", error)
                        return@addSnapshotListener
                    }
                    snapshot?.let {
                        val notifications = it.toObjects(Notification::class.java)
                            .sortedByDescending { it.createdAt }
                        trySend(notifications).isSuccess
                    }
                }
            awaitClose { listener?.remove() }
        }
    }

    suspend fun markNotificationAsSeen(notificationId: String): Result<Unit> = runCatching {
        db.collection("notifications").document(notificationId).update("isSeen", true).await()
    }

    suspend fun markAllNotificationsAsSeen(): Result<Unit> = runCatching {
        val uid = currentUid ?: return@runCatching
        val batch = db.batch()
        val unreadDocs = db.collection("notifications")
            .whereEqualTo("receiverId", uid)
            .whereEqualTo("isSeen", false)
            .get()
            .await()
        unreadDocs.forEach { batch.update(it.reference, "isSeen", true) }
        batch.commit().await()
    }

    // --- Feed & Explore ---
    suspend fun getFeed(limit: Long = 20, lastVisibleTimestamp: Timestamp? = null): Result<List<Post>> = runCatching {
        val uid = currentUid
        var query = db.collection("posts").orderBy("created_at", Query.Direction.DESCENDING)
        if (lastVisibleTimestamp != null) {
            query = query.startAfter(lastVisibleTimestamp)
        }
        val posts = query.limit(limit * 2).get().await().toObjects(Post::class.java)
        // GIỐNG FILE GỐC: Hiện bài approved HOẶC bài của chính mình đăng (dù chưa duyệt)
        posts.filter { it.status == "approved" || it.user_id == uid }.take(limit.toInt())
    }

    suspend fun getUserPosts(uid: String): Result<List<Post>> = runCatching {
        val viewerUid = currentUid
        db.collection("posts")
            .whereEqualTo("user_id", uid)
            .get()
            .await()
            .toObjects(Post::class.java)
            // GIỐNG FILE GỐC: Chỉ khách mới bị chặn bài chưa duyệt, chủ sở hữu thấy hết
            .filter { it.status == "approved" || it.user_id == viewerUid }
            .sortedByDescending { it.created_at }
    }

    suspend fun getPostsByTag(tag: String): Result<List<Post>> = runCatching {
        val uid = currentUid
        db.collection("posts")
            .whereArrayContains("tags", tag)
            .get().await()
            .toObjects(Post::class.java)
            // GIỐNG FILE GỐC
            .filter { it.status == "approved" || it.user_id == uid }
            .sortedByDescending { it.created_at }
    }

    suspend fun getPost(postId: String): Result<Post> = runCatching {
        db.collection("posts").document(postId).get().await().toObject(Post::class.java) ?: throw Exception("Post not found")
    }

    suspend fun getUser(uid: String): User? {
        return db.collection("users").document(uid).get().await().toObject(User::class.java)
    }

    suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        db.collection("users").whereGreaterThanOrEqualTo("username", query).whereLessThanOrEqualTo("username", query + "\uf8ff").get().await().toObjects(User::class.java)
    }

    suspend fun getDomainCounts(): Result<Map<String, Int>> = runCatching {
        val posts = db.collection("posts").orderBy("created_at", Query.Direction.DESCENDING).limit(200).get().await().toObjects(Post::class.java)
        val counts = mutableMapOf<String, Int>()
        posts.forEach { post -> post.tags.forEach { tag -> counts[tag] = counts.getOrDefault(tag, 0) + 1 } }
        counts
    }

    suspend fun updatePremiumStatus(isPremium: Boolean): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        db.collection("users").document(uid).update("is_premium", isPremium).await()
    }

    suspend fun updatePrivateStatus(isPrivate: Boolean): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        db.collection("users").document(uid).update("is_private", isPrivate).await()
    }

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        db.collection("posts").document(postId).delete().await()
    }

    suspend fun updatePostVisibility(postId: String, isPrivate: Boolean): Result<Unit> = runCatching {
        db.collection("posts").document(postId).update("is_private", isPrivate).await()
    }

    suspend fun updatePostCommentsDisabled(postId: String, commentsDisabled: Boolean): Result<Unit> = runCatching {
        db.collection("posts").document(postId).update("comments_disabled", commentsDisabled).await()
    }

    // --- Group Logic ---
    suspend fun getGroups(): Result<List<Group>> = runCatching {
        val snapshot = db.collection("groups").get().await()
        if (snapshot.isEmpty) {
            val defaults = listOf(
                Group("1", "Bodybuilders Club", "Dành cho gymer thích tăng cơ, giảm mỡ và siết cơ chuyên nghiệp.", "Bodybuilding", "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?w=500", 1200, 45, emptyList()),
                Group("2", "Powerlifting Vietnam", "Nơi tập hợp những tín đồ đam mê Bench, Squat, Deadlift cực nặng.", "Powerlifting", "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=500", 850, 32, emptyList()),
                Group("3", "Yoga & Mindfulness", "Tìm lại sự cân bằng, linh hoạt và thư thái sau những giờ tập tạ căng thẳng.", "Yoga", "https://images.unsplash.com/photo-1544367567-0f2fcb009e0b?w=500", 940, 20, emptyList()),
                Group("4", "Calisthenics & Street Workout", "Chinh phục các động tác như Muscle up, Planche, Front lever.", "Calisthenics", "https://images.unsplash.com/photo-1598971639058-fab3c3109a00?w=500", 620, 15, emptyList())
            )
            for (g in defaults) {
                db.collection("groups").document(g.id).set(g).await()
            }
            defaults
        } else {
            snapshot.toObjects(Group::class.java)
        }
    }

    suspend fun getGroupDetail(groupId: String): Result<Group> = runCatching {
        db.collection("groups").document(groupId).get().await().toObject(Group::class.java)
            ?: throw Exception("Group not found")
    }

    suspend fun joinGroup(groupId: String): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val groupRef = db.collection("groups").document(groupId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(groupRef)
            val group = snapshot.toObject(Group::class.java) ?: return@runTransaction
            val currentMemberIds = group.memberIds.toMutableList()
            if (!currentMemberIds.contains(uid)) {
                currentMemberIds.add(uid)
                transaction.update(groupRef, "memberIds", currentMemberIds)
                transaction.update(groupRef, "memberCount", group.memberCount + 1)
            }
        }.await()
    }

    suspend fun leaveGroup(groupId: String): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val groupRef = db.collection("groups").document(groupId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(groupRef)
            val group = snapshot.toObject(Group::class.java) ?: return@runTransaction
            val currentMemberIds = group.memberIds.toMutableList()
            if (currentMemberIds.contains(uid)) {
                currentMemberIds.remove(uid)
                transaction.update(groupRef, "memberIds", currentMemberIds)
                transaction.update(groupRef, "memberCount", (group.memberCount - 1).coerceAtLeast(0))
            }
        }.await()
    }

    suspend fun getGroupPosts(groupId: String): Result<List<Post>> = runCatching {
        val uid = currentUid
        db.collection("posts")
            .whereEqualTo("group_id", groupId)
            .get()
            .await()
            .toObjects(Post::class.java)
            // GIỐNG FILE GỐC
            .filter { it.status == "approved" || it.user_id == uid }
            .sortedByDescending { it.created_at }
    }

    suspend fun createGroupPost(groupId: String, content: String, imageUrl: String = ""): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val docRef = db.collection("posts").document()
        val post = Post(
            id = docRef.id,
            user_id = uid,
            content = content,
            image_url = imageUrl,
            created_at = Timestamp.now(),
            group_id = groupId,
            status = "pending"
        )
        docRef.set(post).await()

        val groupRef = db.collection("groups").document(groupId)
        db.runTransaction { transaction ->
            val group = transaction.get(groupRef).toObject(Group::class.java) ?: return@runTransaction
            transaction.update(groupRef, "postCount", group.postCount + 1)
        }.await()
    }
}