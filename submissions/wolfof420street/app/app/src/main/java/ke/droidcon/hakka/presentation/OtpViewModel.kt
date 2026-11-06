package ke.droidcon.hakka.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ke.droidcon.hakka.domain.model.OtpReducer
import ke.droidcon.hakka.domain.model.OtpState
import ke.droidcon.hakka.domain.usecase.ExtractOtpDigitsUseCase
import ke.droidcon.hakka.domain.usecase.VerifyOtpUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OtpViewModel(
    private val verifyOtpUseCase: VerifyOtpUseCase = VerifyOtpUseCase(),
    private val extractOtpDigitsUseCase: ExtractOtpDigitsUseCase = ExtractOtpDigitsUseCase(),
    externalScope: CoroutineScope? = null,
    initialState: OtpState = OtpState(),
) : ViewModel() {

    private val scope: CoroutineScope by lazy { externalScope ?: viewModelScope }
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<OtpState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        startCountdown()
    }

    fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = scope.launch {
            while (_uiState.value.resendSecondsLeft > 0) {
                delay(1000)
                _uiState.update { OtpReducer.tick(it) }
            }
        }
    }

    fun onDigitInput(index: Int, text: String) {
        _uiState.update { OtpReducer.input(it, index, text) }
    }

    fun onBackspace(index: Int) {
        _uiState.update { OtpReducer.backspace(it, index) }
    }

    fun onPaste(text: String) {
        val digits = extractOtpDigitsUseCase(text)
        _uiState.update { OtpReducer.paste(it, digits) }
    }

    fun onFocusChanged(index: Int) {
        _uiState.update { current ->
            if (current.focusedIndex != index) current.copy(focusedIndex = index) else current
        }
    }

    fun onSubmit() {
        val current = _uiState.value
        if (!current.isComplete) return
        val isValid = verifyOtpUseCase(current.code)
        _uiState.update { OtpReducer.submit(it, isValid) }
    }

    fun onResend() {
        val current = _uiState.value
        if (!current.canResend) return
        _uiState.update { OtpReducer.resend(it) }
        startCountdown()
    }
}
