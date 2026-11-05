package ke.droidcon.hakka.otp

const val OTP_LENGTH = 6
const val RESEND_COUNTDOWN_SECONDS = 30

enum class OtpStatus { Idle, Success, Error }

/** The three visual states a digit box can be in. */
enum class DigitBoxState { Filled, Focused, Empty }

/**
 * Immutable snapshot of the OTP screen. Everything the UI renders is derived from this,
 * and it is only ever produced by [OtpStateHolder].
 */
data class OtpUiState(
    val digits: List<Char?> = List(OTP_LENGTH) { null },
    val focusedIndex: Int = 0,
    val status: OtpStatus = OtpStatus.Idle,
    val resendSecondsLeft: Int = RESEND_COUNTDOWN_SECONDS,
) {
    val code: String get() = digits.filterNotNull().joinToString("")
    val isComplete: Boolean get() = digits.all { it != null }
    val canSubmit: Boolean get() = isComplete
    val isCountingDown: Boolean get() = resendSecondsLeft > 0
    val canResend: Boolean get() = !isCountingDown

    fun boxState(index: Int): DigitBoxState = when {
        index == focusedIndex -> DigitBoxState.Focused
        digits[index] != null -> DigitBoxState.Filled
        else -> DigitBoxState.Empty
    }
}

/** Formats remaining seconds as `M:SS`, e.g. 30 -> "0:30", 5 -> "0:05". */
fun formatCountdown(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

/** Screen-reader label for the box at zero-based [index]: "Digit N of 6". */
fun digitContentDescription(index: Int): String = "Digit ${index + 1} of $OTP_LENGTH"
