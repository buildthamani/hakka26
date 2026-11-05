package ke.droidcon.hakka

const val OTP_LENGTH = 6
const val RESEND_MILLIS = 30_000L

enum class Verification { Idle, Success, Error }

data class OtpState(
    val digits: List<String> = List(OTP_LENGTH) { "" },
    val focus: Int = 0,
    val focusRequest: Int = 0,
    val verification: Verification = Verification.Idle,
    val secondsRemaining: Int = 30,
) {
    val complete: Boolean get() = digits.all { it.length == 1 && it[0] in '0'..'9' }
}

/** Pure input policy: ASCII digits, bounded paste, and explicit focus transitions. */
object OtpLogic {
    fun input(state: OtpState, index: Int, raw: String): OtpState {
        require(index in 0 until OTP_LENGTH)
        if (raw.isEmpty()) return state.copy(
            digits = state.digits.toMutableList().also { it[index] = "" },
            verification = Verification.Idle,
        )
        val numbers = raw.filter { it in '0'..'9' }
        if (numbers.isEmpty()) return state
        // A complete pasted code replaces the entire OTP, regardless of focused box.
        val start = if (numbers.length >= OTP_LENGTH) 0 else index
        val accepted = numbers.take(OTP_LENGTH - start)
        val digits = state.digits.toMutableList()
        accepted.forEachIndexed { offset, digit -> digits[start + offset] = digit.toString() }
        return state.copy(digits = digits, focus = (start + accepted.length).coerceAtMost(5),
            focusRequest = state.focusRequest + 1, verification = Verification.Idle)
    }

    fun backspace(state: OtpState, index: Int): OtpState {
        val target = if (state.digits[index].isEmpty()) (index - 1).coerceAtLeast(0) else index
        return state.copy(digits = state.digits.toMutableList().also { it[target] = "" },
            focus = target, focusRequest = state.focusRequest + 1, verification = Verification.Idle)
    }

    fun submit(state: OtpState): OtpState = when {
        !state.complete -> state
        // Deliberately local verification as required by the challenge; no SMS/backend service.
        state.digits.joinToString("") == "123456" -> state.copy(verification = Verification.Success)
        else -> state.copy(digits = List(OTP_LENGTH) { "" }, focus = 0,
            focusRequest = state.focusRequest + 1, verification = Verification.Error)
    }

    fun remaining(deadline: Long, now: Long): Int =
        ((deadline - now).coerceAtLeast(0).coerceAtMost(RESEND_MILLIS).plus(999) / 1000).toInt()
}
