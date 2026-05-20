package com.socialapp.ui.profile

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.local.LocalCacheManager
import com.socialapp.data.local.CachedProfileData
import com.socialapp.data.model.Post
import com.socialapp.data.model.User
import com.socialapp.data.remote.ImgBBApi
import com.socialapp.data.repository.ChatRepository
import com.socialapp.data.repository.SocialRepository
import com.socialapp.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val followersList: List<User> = emptyList(),
    val followingList: List<User> = emptyList(),
    val friendsList: List<User> = emptyList(),
    val isListLoading: Boolean = false,
    val postedPosts: List<Post> = emptyList(),
    val savedPosts: List<Post> = emptyList(),
    val isPostsLoading: Boolean = false,
    val isFollowing: Boolean = false,
    val friendStatus: String = "none", // none, requested, pending_approval, friends
    val isUploadingAvatar: Boolean = false,
    val isChangingPassword: Boolean = false,
    val passwordChangeSuccess: Boolean = false,
    val passwordChangeError: String? = null,
    val isSharing: Boolean = false,
    val shareSuccess: String? = null,
    val searchResults: List<User> = emptyList(),
    val isSearching: Boolean = false,
    val generatedWorkout: String? = null,
    val isGeneratingWorkout: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = UserRepository()
    private val socialRepo = SocialRepository()
    private val chatRepo = ChatRepository()
    private val cacheManager = LocalCacheManager(application)

    var state by mutableStateOf(ProfileState())
        private set

    init {
        observeSavedPosts()
    }

    private var profileJob: Job? = null

    fun loadProfile(uid: String? = null) {
        val targetUid = uid ?: repo.currentUid ?: return
        profileJob?.cancel()
        
        // Load cache first
        val cached = cacheManager.loadProfileCache(targetUid)
        if (cached != null) {
            state = state.copy(
                user = cached.user,
                postedPosts = cached.postedPosts,
                savedPosts = cached.savedPosts
            )
        } else {
            state = state.copy(isLoading = true, error = null)
        }

        profileJob = viewModelScope.launch {
            repo.getUserSnapshot(targetUid)
                .catch { e -> state = state.copy(isLoading = false, error = e.message) }
                .collect { user ->
                    state = state.copy(isLoading = false, user = user)
                    loadPostedPosts(user.id)
                    if (user.id == repo.currentUid) {
                        loadSavedPosts()
                    } else {
                        checkFollowingStatus(user.id)
                        checkFriendStatus(user.id)
                    }
                }
        }
    }

    private fun checkFollowingStatus(targetUid: String) {
        viewModelScope.launch {
            val following = socialRepo.isFollowing(targetUid)
            state = state.copy(isFollowing = following)
        }
    }

    private fun checkFriendStatus(targetUid: String) {
        viewModelScope.launch {
            val status = socialRepo.getFriendStatus(targetUid)
            state = state.copy(friendStatus = status)
        }
    }

    fun sendFriendRequest() {
        val targetUid = state.user?.id ?: return
        viewModelScope.launch {
            socialRepo.sendFriendRequest(targetUid).onSuccess {
                state = state.copy(friendStatus = "requested")
            }.onFailure {
                state = state.copy(error = it.message)
            }
        }
    }

    fun toggleFollow() {
        val targetUid = state.user?.id ?: return
        viewModelScope.launch {
            socialRepo.toggleFollow(targetUid).onSuccess {
                state = state.copy(isFollowing = it)
            }
        }
    }

    private fun saveToCache() {
        val user = state.user ?: return
        cacheManager.saveProfileCache(user.id, CachedProfileData(
            user = user,
            postedPosts = state.postedPosts,
            savedPosts = state.savedPosts
        ))
    }

    fun loadPostedPosts(uid: String) {
        viewModelScope.launch {
            state = state.copy(isPostsLoading = true)
            socialRepo.getUserPosts(uid)
                .onSuccess { 
                    state = state.copy(isPostsLoading = false, postedPosts = it)
                    saveToCache()
                }
                .onFailure { state = state.copy(isPostsLoading = false) }
        }
    }

    fun observeSavedPosts() {
        viewModelScope.launch {
            socialRepo.getSavedPostIdsFlow().collect {
                val currentUid = repo.currentUid
                if (currentUid != null && (state.user == null || state.user?.id == currentUid)) {
                    loadSavedPosts()
                }
            }
        }
    }

    fun loadSavedPosts() {
        viewModelScope.launch {
            socialRepo.getSavedPosts()
                .onSuccess { 
                    state = state.copy(isPostsLoading = false, savedPosts = it)
                    saveToCache()
                }
                .onFailure { state = state.copy(isPostsLoading = false) }
        }
    }

    fun loadFollowers() {
        val targetUid = state.user?.id ?: return
        viewModelScope.launch {
            state = state.copy(isListLoading = true)
            repo.getFollowers(targetUid)
                .onSuccess { state = state.copy(isListLoading = false, followersList = it) }
                .onFailure { state = state.copy(isListLoading = false) }
        }
    }

    fun loadFollowing() {
        val targetUid = state.user?.id ?: return
        viewModelScope.launch {
            state = state.copy(isListLoading = true)
            repo.getFollowing(targetUid)
                .onSuccess { state = state.copy(isListLoading = false, followingList = it) }
                .onFailure { state = state.copy(isListLoading = false) }
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            state = state.copy(isListLoading = true)
            socialRepo.getFriends()
                .onSuccess { state = state.copy(isListLoading = false, friendsList = it) }
                .onFailure { state = state.copy(isListLoading = false, error = it.message) }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            state = state.copy(searchResults = emptyList())
            return
        }
        viewModelScope.launch {
            state = state.copy(isSearching = true)
            socialRepo.searchUsers(query)
                .onSuccess { state = state.copy(isSearching = false, searchResults = it) }
                .onFailure { state = state.copy(isSearching = false) }
        }
    }

    fun sharePost(post: Post, friend: User) {
        viewModelScope.launch {
            state = state.copy(isSharing = true, shareSuccess = null)
            val shareContent = "Chia sẻ bài viết:\n${post.content}\n${post.image_url}".trim()
            chatRepo.sendMessage(friend.id, shareContent)
                .onSuccess {
                    state = state.copy(isSharing = false, shareSuccess = "Đã gửi đến ${friend.username}")
                }
                .onFailure {
                    state = state.copy(isSharing = false, error = it.message)
                }
        }
    }

    fun toggleEdit() {
        state = state.copy(isEditing = !state.isEditing)
    }

    fun saveProfile(username: String, bio: String, age: Int, hometown: String, birthday: String, hobbies: String, trainingRegime: String) {
        val uid = state.user?.id ?: return
        viewModelScope.launch {
            state = state.copy(isSaving = true)
            // Cần cập nhật UserRepository.updateProfile để nhận các trường này.
            // Tạm thời gọi repo.updateProfile và truyền thêm nếu repo hỗ trợ.
            // Tôi sẽ cập nhật repo sau, hiện tại chỉ copy vào user local.
            repo.updateProfileExt(uid, username, bio, age, hometown, birthday, hobbies, trainingRegime)
                .onSuccess {
                    state = state.copy(
                        isSaving = false,
                        isEditing = false,
                        user = state.user?.copy(
                            username = username, 
                            bio = bio,
                            age = age,
                            hometown = hometown,
                            birthday = birthday,
                            hobbies = hobbies,
                            trainingRegime = trainingRegime
                        )
                    )
                }
                .onFailure { state = state.copy(isSaving = false, error = it.message) }
        }
    }

    fun updateGymMetrics(
        height: Double,
        weight: Double,
        bodyFat: Double,
        benchPr: Double,
        squatPr: Double,
        deadliftPr: Double
    ) {
        val uid = state.user?.id ?: return
        viewModelScope.launch {
            state = state.copy(isSaving = true)
            repo.updateGymMetrics(uid, height, weight, bodyFat, benchPr, squatPr, deadliftPr)
                .onSuccess {
                    state = state.copy(
                        isSaving = false,
                        user = state.user?.copy(
                            height = height,
                            weight = weight,
                            body_fat = bodyFat,
                            bench_pr = benchPr,
                            squat_pr = squatPr,
                            deadlift_pr = deadliftPr
                        )
                    )
                }
                .onFailure { state = state.copy(isSaving = false, error = it.message) }
        }
    }

    fun uploadAvatar(uri: Uri, context: Context) {
        val uid = state.user?.id ?: return
        viewModelScope.launch {
            state = state.copy(isUploadingAvatar = true)
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                val base64 = bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                    ?: throw Exception("Failed to read image")
                val url = ImgBBApi.uploadImage(base64).getOrThrow()
                repo.updateAvatar(uid, url)
                    .onSuccess {
                        state = state.copy(
                            isUploadingAvatar = false,
                            user = state.user?.copy(avatar = url)
                        )
                    }
                    .onFailure { state = state.copy(isUploadingAvatar = false, error = it.message) }
            } catch (e: Exception) {
                state = state.copy(isUploadingAvatar = false, error = e.message)
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            state = state.copy(isChangingPassword = true, passwordChangeError = null, passwordChangeSuccess = false)
            repo.changePassword(currentPassword, newPassword)
                .onSuccess {
                    state = state.copy(isChangingPassword = false, passwordChangeSuccess = true)
                }
                .onFailure {
                    state = state.copy(isChangingPassword = false, passwordChangeError = it.message)
                }
        }
    }

    fun clearPasswordState() {
        state = state.copy(passwordChangeSuccess = false, passwordChangeError = null)
    }

    fun clearShareState() {
        state = state.copy(shareSuccess = null)
    }

    val isOwnProfile: Boolean
        get() = state.user?.id == repo.currentUid

    val currentUid: String?
        get() = repo.currentUid

    val isFollowing: Boolean
        get() = state.isFollowing

    fun generateWorkoutPlan() {
        val user = state.user ?: return
        viewModelScope.launch {
            state = state.copy(isGeneratingWorkout = true)
            val prompt = """
                Hãy đóng vai một chuyên gia thể hình hàng đầu và tạo một kế hoạch tập luyện cá nhân hóa chi tiết dựa trên các chỉ số sau:
                - Chiều cao: ${if(user.height > 0) user.height.toInt().toString() + " cm" else "Chưa cung cấp"}
                - Cân nặng: ${if(user.weight > 0) user.weight.toString() + " kg" else "Chưa cung cấp"}
                - Tỷ lệ mỡ: ${if(user.body_fat > 0) user.body_fat.toString() + "%" else "Chưa cung cấp"}
                - Kỷ lục Bench Press: ${if(user.bench_pr > 0) user.bench_pr.toString() + " kg" else "Chưa cung cấp"}
                - Kỷ lục Squat: ${if(user.squat_pr > 0) user.squat_pr.toString() + " kg" else "Chưa cung cấp"}
                - Kỷ lục Deadlift: ${if(user.deadlift_pr > 0) user.deadlift_pr.toString() + " kg" else "Chưa cung cấp"}
                Hãy đưa ra lịch tập trong tuần cụ thể, số sets/reps và lời khuyên dinh dưỡng ngắn gọn phù hợp với thể trạng của họ.
            """.trimIndent()

            com.socialapp.data.remote.GeminiApi.generateContent(prompt).onSuccess { result ->
                state = state.copy(generatedWorkout = result, isGeneratingWorkout = false)
            }.onFailure {
                state = state.copy(generatedWorkout = "Không thể kết nối đến máy chủ AI. Vui lòng kiểm tra lại kết nối mạng.", isGeneratingWorkout = false)
            }
        }
    }

    fun clearGeneratedWorkout() {
        state = state.copy(generatedWorkout = null)
    }
}
