package ke.droidcon.hakka.otp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OtpReducer {
    private val _state = MutableStateFlow(OtpState())
    val state: StateFlow<OtpState> = _state.asStateFlow()

    fun onDigitsChanged(newDigits: String) {
        _state.update { currentState ->
            // Extract only digits to seamlessly support copy/paste like "Your code is 482913"
            val sanitized = newDigits.filter { it.isDigit() }.take(6)
            currentState.copy(
                digits = sanitized,
                // Reset error status if the user starts typing again
                status = if (currentState.status == OtpStatus.Error) OtpStatus.Idle else currentState.status
            )
        }
    }

    fun onTick() {
        _state.update { currentState ->
            if (currentState.secondsLeft > 0) {
                currentState.copy(secondsLeft = currentState.secondsLeft - 1)
            } else {
                currentState
            }
        }
    }

    fun onResend() {
        _state.update { currentState ->
            if (currentState.canResend) {
                // Restart timer to 30s, clear digits, and reset to Idle
                currentState.copy(digits = "", secondsLeft = 30, status = OtpStatus.Idle)
            } else {
                currentState
            }
        }
    }

    fun onSubmit() {
        _state.update { currentState ->
            if (currentState.isSubmitEnabled) {
                if (currentState.digits == "123456") {
                    currentState.copy(status = OtpStatus.Success)
                } else {
                    // Code didn't work. Show error, clear digits, let UI re-focus on digit 1.
                    currentState.copy(status = OtpStatus.Error, digits = "")
                }
            } else {
                currentState
            }
        }
    }
}
