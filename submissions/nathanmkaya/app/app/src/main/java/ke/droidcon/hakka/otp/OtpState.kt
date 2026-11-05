package ke.droidcon.hakka.otp

const val OTP_LENGTH = 6
const val RESEND_COOLDOWN_SECONDS = 30

/** The demo has no backend: issue #1 defines this as the only code that verifies. */
internal const val ACCEPTED_CODE = "123456"

private val EMPTY_DIGITS: List<Char?> = List(OTP_LENGTH) { null }

enum class OtpStatus { Idle, Success, Error }

data class OtpState(
    val digits: List<Char?> = EMPTY_DIGITS,
    val focusedIndex: Int = 0,
    val status: OtpStatus = OtpStatus.Idle,
    val resendSecondsLeft: Int = RESEND_COOLDOWN_SECONDS,
) {
    val code: String get() = digits.filterNotNull().joinToString("")
    val isComplete: Boolean get() = digits.none { it == null }
    val canResend: Boolean get() = resendSecondsLeft == 0
}

sealed interface OtpAction {
    /** Raw text typed or pasted into the box at [index]; it may hold non-digits or a whole SMS. */
    data class Input(val index: Int, val text: String) : OtpAction
    data class Backspace(val index: Int) : OtpAction
    data class Focus(val index: Int) : OtpAction
    data object Submit : OtpAction
    data object Tick : OtpAction
    data object Resend : OtpAction
}

fun reduce(state: OtpState, action: OtpAction): OtpState = when (action) {
    is OtpAction.Input -> state.input(action.index, action.text)
    is OtpAction.Backspace -> state.backspace(action.index)
    is OtpAction.Focus -> state.copy(focusedIndex = action.index)
    OtpAction.Submit -> state.submit()
    OtpAction.Tick -> state.copy(resendSecondsLeft = (state.resendSecondsLeft - 1).coerceAtLeast(0))
    OtpAction.Resend -> if (state.canResend) state.copy(resendSecondsLeft = RESEND_COOLDOWN_SECONDS) else state
}

private fun OtpState.input(index: Int, text: String): OtpState {
    val incoming = text.filter { it in '0'..'9' }
    if (incoming.isEmpty()) return this
    // A paste that carries a whole code fills every box, whichever box received it.
    val start = if (incoming.length >= OTP_LENGTH) 0 else index
    val written = incoming.take(OTP_LENGTH - start)
    val updated = digits.toMutableList()
    written.forEachIndexed { offset, digit -> updated[start + offset] = digit }
    return copy(
        digits = updated,
        focusedIndex = (start + written.length).coerceAtMost(OTP_LENGTH - 1),
        status = OtpStatus.Idle,
    )
}

private fun OtpState.backspace(index: Int): OtpState {
    val cleared = when {
        digits[index] != null -> index
        index > 0 -> index - 1
        else -> return this
    }
    return copy(
        digits = digits.toMutableList().also { it[cleared] = null },
        focusedIndex = cleared,
        status = OtpStatus.Idle,
    )
}

private fun OtpState.submit(): OtpState = when {
    !isComplete -> this
    code == ACCEPTED_CODE -> copy(status = OtpStatus.Success)
    else -> copy(digits = EMPTY_DIGITS, focusedIndex = 0, status = OtpStatus.Error)
}
