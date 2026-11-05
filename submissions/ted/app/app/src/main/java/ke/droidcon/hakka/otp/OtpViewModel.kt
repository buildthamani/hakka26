package ke.droidcon.hakka.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val CODE_LENGTH = 6
const val RESEND_SECONDS = 30
private const val VALID_CODE = "123456"

enum class OtpStatus { Idle, Success, Error }

/** Everything the OTP screen shows, derived from the six-digit code, the last submission and the resend timer. */
data class OtpUiState(
    val code: String = "",
    val status: OtpStatus = OtpStatus.Idle,
    val secondsLeft: Int = RESEND_SECONDS,
) {
    /** The first empty box, where the next digit goes; null once all six are filled. */
    val focusedIndex: Int? get() = code.length.takeIf { it < CODE_LENGTH }
    val isComplete: Boolean get() = code.length == CODE_LENGTH
    val canResend: Boolean get() = secondsLeft == 0
    val resendLabel: String
        get() = if (canResend) "Resend" else "Resend in 0:${secondsLeft.toString().padStart(2, '0')}"
    val statusMessage: String
        get() = when (status) {
            OtpStatus.Idle -> ""
            OtpStatus.Success -> "You're in."
            OtpStatus.Error -> "That code didn't work. Try again."
        }
}

/** Keeps the digits of [text], in order, up to [CODE_LENGTH]: "Your code is 482913" becomes "482913". */
fun extractDigits(text: String): String = text.filter(Char::isDigit).take(CODE_LENGTH)

class OtpViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private var countdown: Job? = null

    init {
        startCountdown()
    }

    /**
     * Called with the full contents of the hidden text field after every edit. The field's cursor always sits
     * at the end, so a typed digit lands in the first empty box and focus moves to the next one, backspace on
     * that empty box removes the previous digit and moves back, and pasted text is reduced to its digits.
     * Anything that is not a digit, or beyond the sixth digit, is dropped.
     */
    fun onCodeChange(input: String) {
        _uiState.update { it.copy(code = extractDigits(input), status = OtpStatus.Idle) }
    }

    fun onSubmit() {
        _uiState.update { state ->
            when {
                !state.isComplete -> state
                state.code == VALID_CODE -> state.copy(status = OtpStatus.Success)
                // Clearing the code empties every box, which puts focus back on the first one.
                else -> state.copy(code = "", status = OtpStatus.Error)
            }
        }
    }

    fun onResend() {
        if (_uiState.value.canResend) startCountdown()
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            _uiState.update { it.copy(secondsLeft = RESEND_SECONDS) }
            while (_uiState.value.secondsLeft > 0) {
                delay(1_000)
                _uiState.update { it.copy(secondsLeft = it.secondsLeft - 1) }
            }
        }
    }
}
