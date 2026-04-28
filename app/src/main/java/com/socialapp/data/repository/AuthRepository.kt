package com.socialapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.socialapp.data.model.User
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw Exception("Login failed")

        // KIỂM TRA QUYỀN CHẶN:
        val userDoc = db.collection("users").document(uid).get().await()
        val user = userDoc.toObject(User::class.java)
        if (user?.is_blocked == true) {
            auth.signOut() // Đăng xuất ngay lập tức
            throw Exception("Tài khoản của bạn đã bị khóa bởi quản trị viên!")
        }
    }

    suspend fun register(username: String, email: String, password: String): Result<Unit> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw Exception("Registration failed")
        val user = User(
            id = uid,
            username = username,
            email = email,
            role = "user",
            is_blocked = false // Mặc định không bị chặn
        )
        db.collection("users").document(uid).set(user).await()
    }

    fun logout() {
        auth.signOut()
    }
}