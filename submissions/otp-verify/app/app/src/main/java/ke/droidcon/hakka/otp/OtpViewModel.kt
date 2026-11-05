package ke.droidcon.hakka.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OtpViewModel(
    private val verifier: OtpVerifier,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        startCountdown()
    }

    fun onFieldChanged(index: Int, rawText: String) {
        _uiState.update { OtpReducer.fieldChanged(it, index, rawText) }
    }

    fun onBackspace(index: Int) {
        _uiState.update { OtpReducer.backspace(it, index) }
    }

    fun onFocusChanged(index: Int) {
        _uiState.update { state ->
            if (state.focusedIndex == index) state else OtpReducer.focus(state, index)
        }
    }

    fun onVerify() {
        val submitted = _uiState.value
        if (!submitted.canSubmit) return

        _uiState.update(OtpReducer::startVerifying)
        viewModelScope.launch {
            val result = verifier.verify(submitted.code)
            _uiState.update { OtpReducer.applyVerificationResult(it, result) }
        }
    }

    fun onResend() {
        if (!_uiState.value.canResend) return
        _uiState.update(OtpReducer::resend)
        startCountdown()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_uiState.value.secondsUntilResend > 0) {
                delay(1_000)
                _uiState.update(OtpReducer::tick)
            }
        }
    }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val verifier: OtpVerifier,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(OtpViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return OtpViewModel(verifier) as T
        }
    }
}
