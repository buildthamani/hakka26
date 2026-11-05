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

const val OTP_LENGTH = 6
const val RESEND_COOLDOWN_SECONDS = 30
const val DEMO_VALID_CODE = "123456"

enum class OtpStatus { None, Success, Error }

data class OtpUiState(
    val digits: List<String> = List(OTP_LENGTH) { "" },
    val focusedIndex: Int = 0,
    val status: OtpStatus = OtpStatus.None,
    val resendSecondsLeft: Int = RESEND_COOLDOWN_SECONDS,
) {
    val code: String get() = digits.joinToString("")
    val isComplete: Boolean get() = digits.all { it.isNotEmpty() }
    val canResend: Boolean get() = resendSecondsLeft == 0
}

/** Formats a number of seconds as `M:SS`, e.g. 30 -> "0:30", 5 -> "0:05". */
fun formatCountdown(totalSeconds: Int): String =
    "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

class OtpViewModel(
    private val isValidCode: (String) -> Boolean = { it == DEMO_VALID_CODE },
    private val resendCooldownSeconds: Int = RESEND_COOLDOWN_SECONDS,
) : ViewModel() {

    private val _state = MutableStateFlow(OtpUiState(resendSecondsLeft = resendCooldownSeconds))
    val state: StateFlow<OtpUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    init {
        startResendCountdown()
    }

    /**
     * Handles the raw text of the box at [index] after an edit. The box already holds at most
     * one digit, so anything beyond it is either a newly typed character or pasted text.
     * Returns the index of the box that should now have focus.
     */
    fun onBoxTextChange(index: Int, text: String): Int {
        val current = _state.value.digits[index]
        if (text == current) return _state.value.focusedIndex
        if (text.isEmpty()) {
            setDigits { it[index] = "" }
            return _state.value.focusedIndex
        }
        val typed = when {
            current.isEmpty() -> text
            text.startsWith(current) -> text.drop(current.length)
            text.endsWith(current) -> text.dropLast(current.length)
            else -> text
        }
        val newDigits = typed.filter(Char::isDigit)
        when {
            newDigits.isEmpty() -> Unit
            newDigits.length == 1 -> {
                // Fast typing can reach a filled box before focus has moved on; send it to the
                // box focus was already advanced to rather than overwriting the previous digit.
                val focused = _state.value.focusedIndex
                val target = if (current.isNotEmpty() && focused != index) focused else index
                setDigits(focusIndex = (target + 1).coerceAtMost(OTP_LENGTH - 1)) {
                    it[target] = newDigits
                }
            }
            else -> onPaste(index, newDigits)
        }
        return _state.value.focusedIndex
    }

    /** Fills boxes from pasted text, keeping only its digits. A full code always fills from box 1. */
    fun onPaste(index: Int, pasted: String) {
        val pastedDigits = pasted.filter(Char::isDigit)
        if (pastedDigits.isEmpty()) return
        val start = if (pastedDigits.length >= OTP_LENGTH) 0 else index
        val fill = pastedDigits.take(OTP_LENGTH - start)
        setDigits(focusIndex = (start + fill.length).coerceAtMost(OTP_LENGTH - 1)) { digits ->
            fill.forEachIndexed { offset, digit -> digits[start + offset] = digit.toString() }
        }
    }

    /** Backspace pressed on an empty box: move back and clear the previous digit. Returns the box to focus. */
    fun onBackspaceOnEmptyBox(index: Int): Int {
        if (index > 0 && _state.value.digits[index].isEmpty()) {
            setDigits(focusIndex = index - 1) { it[index - 1] = "" }
        }
        return _state.value.focusedIndex
    }

    fun onBoxFocused(index: Int) {
        _state.update { it.copy(focusedIndex = index) }
    }

    fun onSubmit() {
        val current = _state.value
        if (!current.isComplete) return
        if (isValidCode(current.code)) {
            _state.update { it.copy(status = OtpStatus.Success) }
        } else {
            _state.update {
                it.copy(
                    digits = List(OTP_LENGTH) { "" },
                    focusedIndex = 0,
                    status = OtpStatus.Error,
                )
            }
        }
    }

    fun onResend() {
        if (!_state.value.canResend) return
        _state.update { it.copy(status = OtpStatus.None) }
        startResendCountdown()
    }

    private fun startResendCountdown() {
        countdownJob?.cancel()
        _state.update { it.copy(resendSecondsLeft = resendCooldownSeconds) }
        countdownJob = viewModelScope.launch {
            while (_state.value.resendSecondsLeft > 0) {
                delay(1_000)
                _state.update { it.copy(resendSecondsLeft = it.resendSecondsLeft - 1) }
            }
        }
    }

    private inline fun setDigits(
        focusIndex: Int? = null,
        edit: (MutableList<String>) -> Unit,
    ) {
        _state.update { state ->
            state.copy(
                digits = state.digits.toMutableList().also(edit),
                focusedIndex = focusIndex ?: state.focusedIndex,
                status = OtpStatus.None,
            )
        }
    }
}
