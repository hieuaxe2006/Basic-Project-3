package com.socialapp.ui.premium

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.socialapp.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class PremiumState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)

class PremiumViewModel : ViewModel() {
    private val repo = UserRepository()
    var state by mutableStateOf(PremiumState())
        private set

    fun upgrade() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            // Giả lập xử lý thanh toán thực tế
            delay(2000)

            // Gọi hàm nâng cấp với số tiền là 199.000đ
            repo.upgradeToPremium(amount = 199000L).onSuccess {
                state = state.copy(isLoading = false, isSuccess = true)
            }.onFailure {
                state = state.copy(isLoading = false, error = it.message)
            }
        }
    }
}