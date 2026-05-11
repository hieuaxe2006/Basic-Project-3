package com.socialapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.socialapp.data.model.Message
import com.socialapp.data.model.User
import kotlinx.coroutines.tasks.await

// Cấu trúc đối tác chat: Thêm unreadCount để hiển thị tín hiệu tin nhắn mới
data class ChatPartner(
    val user: User,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0
)

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    val currentUid get() = auth.currentUser?.uid

    // Gửi tin nhắn: Mặc định seen = false
    suspend fun sendMessage(receiverId: String, content: String): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val docRef = db.collection("messages").document()
        val message = Message(
            id = docRef.id,
            sender_id = uid,
            receiver_id = receiverId,
            content = content,
            timestamp = Timestamp.now(),
            seen = false
        )
        docRef.set(message).await()
    }

    // Đánh dấu đã xem: Tìm các tin nhắn đối phương gửi cho mình mà chưa xem -> chuyển thành true
    suspend fun markAsRead(otherUid: String): Result<Unit> = runCatching {
        val uid = currentUid ?: return@runCatching
        val unreadQuery = db.collection("messages")
            .whereEqualTo("sender_id", otherUid)
            .whereEqualTo("receiver_id", uid)
            .whereEqualTo("seen", false)
            .get().await()

        if (!unreadQuery.isEmpty) {
            val batch = db.batch()
            unreadQuery.documents.forEach { doc ->
                batch.update(doc.reference, "seen", true)
            }
            batch.commit().await()
        }
    }

    // Lắng nghe tin nhắn thời gian thực
    fun listenMessages(
        otherUid: String,
        onUpdate: (List<Message>) -> Unit
    ): ListenerRegistration {
        val uid = currentUid ?: return db.collection("_empty").addSnapshotListener { _, _ -> }

        return db.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val all = snapshot?.toObjects(Message::class.java) ?: emptyList()
                val filtered = all.filter {
                    (it.sender_id == uid && it.receiver_id == otherUid) ||
                            (it.sender_id == otherUid && it.receiver_id == uid)
                }
                onUpdate(filtered)
            }
    }

    // Lấy danh sách các cuộc trò chuyện và đếm tin nhắn chưa đọc
    suspend fun getChatPartners(): Result<List<ChatPartner>> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")

        // Lấy tất cả tin nhắn liên quan đến tôi
        val sent = db.collection("messages")
            .whereEqualTo("sender_id", uid)
            .get().await()
            .toObjects(Message::class.java)

        val received = db.collection("messages")
            .whereEqualTo("receiver_id", uid)
            .get().await()
            .toObjects(Message::class.java)

        val allMessages = (sent + received).sortedByDescending { it.timestamp }
        val partnerIds = (sent.map { it.receiver_id } + received.map { it.sender_id })
            .distinct()
            .filter { it != uid }

        val partners = mutableListOf<ChatPartner>()
        partnerIds.forEach { pid ->
            val doc = db.collection("users").document(pid).get().await()
            val user = doc.toObject(User::class.java)?.copy(id = doc.id)
            if (user != null) {
                val lastMsg = allMessages.firstOrNull { it.sender_id == pid || it.receiver_id == pid }

                // Đếm số tin nhắn từ đối tác này gửi cho mình mà mình chưa xem
                val unreadCount = received.count { it.sender_id == pid && !it.seen }

                partners.add(ChatPartner(user, lastMsg, unreadCount))
            }
        }
        // Sắp xếp danh sách: Cuộc trò chuyện có tin nhắn mới nhất lên đầu
        partners.sortedByDescending { it.lastMessage?.timestamp }
    }

    suspend fun getCurrentUser(): User? {
        val uid = currentUid ?: return null
        return db.collection("users").document(uid).get().await()
            .toObject(User::class.java)?.copy(id = uid)
    }

    suspend fun updateNote(note: String): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        db.collection("users").document(uid).update("note", note).await()
    }

    suspend fun searchUsers(query: String): Result<List<User>> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val snapshot = db.collection("users")
            .orderBy("username")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(20)
            .get().await()
        snapshot.toObjects(User::class.java)
            .filter { it.id != uid }
    }
}