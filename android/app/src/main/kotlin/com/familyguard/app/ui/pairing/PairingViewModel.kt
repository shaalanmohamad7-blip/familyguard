package com.familyguard.app.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyguard.app.data.repository.PairingRepository
import com.familyguard.app.data.repository.PairingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PairingUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val paired: Boolean = false
)

class PairingViewModel(private val pairingRepository: PairingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    fun onCodeChanged(code: String) {
        _uiState.value = _uiState.value.copy(code = code, errorMessage = null)
    }

    fun submit() {
        val currentState = _uiState.value
        if (currentState.isLoading) return

        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = pairingRepository.redeemCode(currentState.code)) {
                is PairingResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, paired = true)
                }
                is PairingResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
