package com.socialapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.socialapp.data.model.User
import kotlinx.coroutines.tasks.await

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser get() = auth.currentUser

    suspend fun login(email: String, password: String): Result<User> = runCatching {
        android.util.Log.d("GYMHUB_AUTH", "--- BẮT ĐẦU ĐĂNG NHẬP ---")
        android.util.Log.d("GYMHUB_AUTH", "Email: $email")
        kotlinx.coroutines.withTimeout(8000L) {
            android.util.Log.d("GYMHUB_AUTH", "1. Đang gọi signInWithEmailAndPassword...")
            val result = auth.signInWithEmailAndPassword(email, password).safeAwait()
            val uid = result.user?.uid ?: throw Exception("Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.")
            android.util.Log.d("GYMHUB_AUTH", "2. Đăng nhập Auth thành công. UID: $uid")

            android.util.Log.d("GYMHUB_AUTH", "3. Đang truy vấn dữ liệu Firestore cho user...")
            val userDoc = db.collection("users").document(uid).get().safeAwait()
            android.util.Log.d("GYMHUB_AUTH", "4. Truy vấn Firestore thành công.")
            val user = userDoc.toObject(User::class.java) ?: throw Exception("Không tìm thấy dữ liệu người dùng.")

            if (user.is_blocked) {
                android.util.Log.d("GYMHUB_AUTH", "Lỗi: Tài khoản bị khóa.")
                auth.signOut()
                throw Exception("Tài khoản của bạn đã bị khóa bởi quản trị viên!")
            }
            android.util.Log.d("GYMHUB_AUTH", "--- ĐĂNG NHẬP THÀNH CÔNG ---")
            user
        }
    }.onFailure {
        android.util.Log.e("GYMHUB_AUTH", "LỖI ĐĂNG NHẬP: ${it.message}", it)
    }

    suspend fun register(username: String, email: String, password: String): Result<Unit> = runCatching {
        android.util.Log.d("GYMHUB_AUTH", "--- BẮT ĐẦU ĐĂNG KÝ ---")
        android.util.Log.d("GYMHUB_AUTH", "Username: $username, Email: $email")
        kotlinx.coroutines.withTimeout(8000L) {
            android.util.Log.d("GYMHUB_AUTH", "1. Đang tạo tài khoản Auth...")
            val result = auth.createUserWithEmailAndPassword(email, password).safeAwait()
            val uid = result.user?.uid ?: throw Exception("Đăng ký thất bại. Vui lòng thử lại.")
            android.util.Log.d("GYMHUB_AUTH", "2. Tạo tài khoản Auth thành công. UID: $uid")

            val user = User(
                id = uid,
                username = username,
                email = email,
                role = "user",
                is_blocked = false
            )
            android.util.Log.d("GYMHUB_AUTH", "3. Đang ghi dữ liệu người dùng vào Firestore...")
            db.collection("users").document(uid).set(user).safeAwait()
            android.util.Log.d("GYMHUB_AUTH", "4. Ghi dữ liệu Firestore thành công.")
            android.util.Log.d("GYMHUB_AUTH", "--- ĐĂNG KÝ THÀNH CÔNG ---")
            Unit
        }
    }.onFailure {
        android.util.Log.e("GYMHUB_AUTH", "LỖI ĐĂNG KÝ: ${it.message}", it)
    }

    fun logout() {
        auth.signOut()
    }
}

// Custom extension function to safely await Tasks using native suspendCancellableCoroutine
private suspend fun <T> com.google.android.gms.tasks.Task<T>.safeAwait(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            val exception = task.exception ?: Exception("Tác vụ thất bại hoặc bị hủy.")
            continuation.resumeWithException(exception)
        }
    }
}