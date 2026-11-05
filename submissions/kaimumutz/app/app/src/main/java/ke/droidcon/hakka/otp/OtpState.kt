package ke.droidcon.hakka.otp

const val OTP_LENGTH = 6
const val RESEND_COOLDOWN_SECONDS = 30

/** Result of the last submit, shown in the status line. */
enum class OtpStatus { None, Success, Error }

/** How a single digit circle is drawn. */
enum class DigitState { Filled, Focused, Empty }

/**
 * Immutable state of the OTP sheet. Every user action is a pure function that returns the next
 * state, so all behaviour lives here and is unit-tested without Android.
 *
 * [focusedIndex] ranges over `0..OTP_LENGTH`; `OTP_LENGTH` means "past the last box" (code complete,
 * no box focused).
 */
data class OtpState(
    val digits: List<Char?> = List(OTP_LENGTH) { null },
    val focusedIndex: Int = 0,
    val status: OtpStatus = OtpStatus.None,
    val resendSecondsLeft: Int = RESEND_COOLDOWN_SECONDS,
) {
    init {
        require(digits.size == OTP_LENGTH) { "Expected $OTP_LENGTH digits" }
        require(focusedIndex in 0..OTP_LENGTH) { "focusedIndex out of range: $focusedIndex" }
    }

    val code: String get() = digits.joinToString("") { it?.toString().orEmpty() }
    val isComplete: Boolean get() = digits.all { it != null }
    val canSubmit: Boolean get() = isComplete
    val canResend: Boolean get() = resendSecondsLeft == 0

    fun digitState(index: Int): DigitState = when {
        index == focusedIndex -> DigitState.Focused
        digits[index] != null -> DigitState.Filled
        else -> DigitState.Empty
    }

    /** Types [char] into the focused box and advances focus. Non-digits are rejected. */
    fun typeDigit(char: Char): OtpState {
        if (!char.isDigit() || focusedIndex == OTP_LENGTH) return this
        return copy(
            digits = digits.replaceAt(focusedIndex, char),
            focusedIndex = focusedIndex + 1,
            status = OtpStatus.None,
        )
    }

    /**
     * Clears the focused box if it holds a digit; on an empty box, moves back one box and clears
     * the previous digit.
     */
    fun backspace(): OtpState {
        if (focusedIndex < OTP_LENGTH && digits[focusedIndex] != null) {
            return copy(digits = digits.replaceAt(focusedIndex, null), status = OtpStatus.None)
        }
        if (focusedIndex == 0) return this
        val previous = focusedIndex - 1
        return copy(
            digits = digits.replaceAt(previous, null),
            focusedIndex = previous,
            status = OtpStatus.None,
        )
    }

    /**
     * Extracts the digits from pasted [text] (e.g. "Your code is 482913"). A full code fills every
     * box; a shorter run is inserted from the focused box onwards.
     */
    fun paste(text: String): OtpState {
        val pasted = text.filter(Char::isDigit)
        if (pasted.isEmpty()) return this
        val start = if (pasted.length >= OTP_LENGTH) 0 else focusedIndex.coerceAtMost(OTP_LENGTH - 1)
        val updated = digits.toMutableList()
        pasted.take(OTP_LENGTH - start).forEachIndexed { offset, digit -> updated[start + offset] = digit }
        return copy(
            digits = updated,
            focusedIndex = (start + pasted.length).coerceAtMost(OTP_LENGTH),
            status = OtpStatus.None,
        )
    }

    /** Moves focus to the box the user tapped. */
    fun focus(index: Int): OtpState = copy(focusedIndex = index.coerceIn(0, OTP_LENGTH - 1))

    /**
     * Verifies a complete code. Success keeps the digits; failure clears every box and returns
     * focus to the first one.
     */
    fun submit(verifier: OtpVerifier): OtpState {
        if (!canSubmit) return this
        return if (verifier.verify(code)) {
            copy(status = OtpStatus.Success, focusedIndex = OTP_LENGTH)
        } else {
            OtpState(status = OtpStatus.Error, resendSecondsLeft = resendSecondsLeft)
        }
    }

    /** Advances the resend countdown by one second. */
    fun tick(): OtpState =
        if (resendSecondsLeft == 0) this else copy(resendSecondsLeft = resendSecondsLeft - 1)

    /** Requests a new code; only allowed once the countdown has finished, and restarts it. */
    fun resend(): OtpState =
        if (!canResend) this else copy(resendSecondsLeft = RESEND_COOLDOWN_SECONDS, status = OtpStatus.None)
}

/** Formats remaining seconds as `M:SS`, e.g. 30 → "0:30", 5 → "0:05". */
fun formatCountdown(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

private fun <T> List<T>.replaceAt(index: Int, value: T): List<T> =
    toMutableList().also { it[index] = value }
