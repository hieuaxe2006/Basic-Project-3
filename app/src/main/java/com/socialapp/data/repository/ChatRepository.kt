package com.socialapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.socialapp.data.model.Message
import com.socialapp.data.model.User
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    val currentUid get() = auth.currentUser?.uid

    suspend fun sendMessage(receiverId: String, content: String): Result<Unit> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")
        val docRef = db.collection("messages").document()
        val message = Message(
            id = docRef.id,
            sender_id = uid,
            receiver_id = receiverId,
            content = content,
            timestamp = Timestamp.now()
        )
        docRef.set(message).await()
    }

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

    suspend fun getChatPartners(): Result<List<User>> = runCatching {
        val uid = currentUid ?: throw Exception("Not logged in")

        val sent = db.collection("messages")
            .whereEqualTo("sender_id", uid)
            .get().await()
            .toObjects(Message::class.java)

        val received = db.collection("messages")
            .whereEqualTo("receiver_id", uid)
            .get().await()
            .toObjects(Message::class.java)

        val partnerIds = (sent.map { it.receiver_id } + received.map { it.sender_id })
            .distinct()
            .filter { it != uid }

        val users = mutableListOf<User>()
        partnerIds.forEach { pid ->
            val doc = db.collection("users").document(pid).get().await()
            doc.toObject(User::class.java)?.copy(id = doc.id)?.let { users.add(it) }
        }
        users
    }

    suspend fun getUser(uid: String): User? {
        return db.collection("users").document(uid).get().await()
            .toObject(User::class.java)?.copy(id = uid)
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
