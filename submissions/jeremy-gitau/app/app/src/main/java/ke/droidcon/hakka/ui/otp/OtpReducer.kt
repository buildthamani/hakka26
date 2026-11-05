package ke.droidcon.hakka.ui.otp

import ke.droidcon.hakka.domain.OtpVerifier

const val OTP_LENGTH = 6
const val RESEND_SECONDS = 30

enum class OtpStatus { Idle, Success, Error }

data class OtpState(
    val code: String = "",
    val status: OtpStatus = OtpStatus.Idle,
    val resendSecondsLeft: Int = RESEND_SECONDS,
) {
    val canSubmit: Boolean get() = code.length == OTP_LENGTH
    val canResend: Boolean get() = resendSecondsLeft == 0

    /** Index of the box that receives the next digit, or -1 when all are filled. */
    val focusedIndex: Int get() = if (code.length < OTP_LENGTH) code.length else -1
}

sealed interface OtpAction {
    /** New raw text from the input field (typing, backspace or paste). */
    data class Input(val text: String) : OtpAction
    data object Submit : OtpAction
    data object Tick : OtpAction
    data object Resend : OtpAction
}

/** Pure state machine for the OTP screen: no Android or Compose types. */
class OtpReducer(private val verifier: OtpVerifier) {

    fun reduce(state: OtpState, action: OtpAction): OtpState = when (action) {
        is OtpAction.Input -> {
            val next = nextCode(state.code, action.text)
            if (next == state.code) state else state.copy(code = next, status = OtpStatus.Idle)
        }
        OtpAction.Submit -> submit(state)
        OtpAction.Tick -> state.copy(resendSecondsLeft = (state.resendSecondsLeft - 1).coerceAtLeast(0))
        OtpAction.Resend -> if (state.canResend) state.copy(resendSecondsLeft = RESEND_SECONDS) else state
    }

    private fun submit(state: OtpState): OtpState = when {
        !state.canSubmit -> state
        verifier.verify(state.code) -> state.copy(status = OtpStatus.Success)
        else -> state.copy(code = "", status = OtpStatus.Error)
    }

    /**
     * Keeps digits only. A multi-character insert (a paste such as "Your code is 482913")
     * replaces the code with its last six digits; a single extra keystroke on a full code is ignored.
     */
    private fun nextCode(current: String, text: String): String {
        val digits = text.filter { it in '0'..'9' }
        return when {
            digits.length <= OTP_LENGTH -> digits
            digits.length - current.length > 1 -> digits.takeLast(OTP_LENGTH)
            else -> current
        }
    }
}

/** Formats remaining seconds as "0:SS". */
fun formatCountdown(seconds: Int): String = "0:" + seconds.coerceIn(0, 59).toString().padStart(2, '0')
