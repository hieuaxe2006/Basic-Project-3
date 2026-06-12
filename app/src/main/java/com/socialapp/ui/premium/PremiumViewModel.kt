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
    val isSuccess: Boolean = false, // Chỉ dùng để hiện màn hình "Nâng cấp thành công"
    val error: String? = null,
    val showCheckoutSheet: Boolean = false,
    val selectedMethod: String? = null
)

class PremiumViewModel : ViewModel() {
    private val repo = UserRepository()
    var state by mutableStateOf(PremiumState())
        private set

    fun showCheckout(method: String?) {
        state = state.copy(showCheckoutSheet = method != null, selectedMethod = method)
    }

    fun upgrade() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null, showCheckoutSheet = false)

            // Giả lập thời gian chờ thanh toán
            delay(2000)

            repo.upgradeToPremium(amount = 199000L).onSuccess {
                state = state.copy(isLoading = false, isSuccess = true)
            }.onFailure {
                state = state.copy(isLoading = false, error = it.message)
            }
        }
    }

    fun resetSuccess() {
        state = state.copy(isSuccess = false)
    }
}