package ke.droidcon.hakka

/** Outcome of the last submit attempt. */
enum class OtpStatus { None, Success, Error }

/**
 * Pure-Kotlin, immutable state for the OTP sheet. Every operation returns a new state,
 * so it can be unit tested without Android or Compose.
 */
data class OtpState(
    val digits: List<Char?> = List(LENGTH) { null },
    val focusedIndex: Int = 0,
    val status: OtpStatus = OtpStatus.None,
    val resendSeconds: Int = RESEND_SECONDS,
) {
    val code: String get() = digits.filterNotNull().joinToString("")
    val isComplete: Boolean get() = digits.all { it != null }
    val canSubmit: Boolean get() = isComplete
    val canResend: Boolean get() = resendSeconds <= 0

    /** "Resend in 0:SS" while counting down, "Resend" once available. */
    val resendLabel: String
        get() = if (canResend) "Resend" else "Resend in ${formatCountdown(resendSeconds)}"

    val statusMessage: String?
        get() = when (status) {
            OtpStatus.None -> null
            OtpStatus.Success -> SUCCESS_MESSAGE
            OtpStatus.Error -> ERROR_MESSAGE
        }

    fun focus(index: Int): OtpState = copy(focusedIndex = index.coerceIn(0, LENGTH - 1))

    /**
     * Text changed in box [index]. Non-digits are dropped. One digit fills the box and
     * advances; several digits (a paste) fill consecutive boxes from [index]; no digits clears the box.
     */
    fun onInput(index: Int, raw: String): OtpState {
        if (index !in 0 until LENGTH) return this
        var input = raw.filter { it in '0'..'9' }
        // Typing into an already-filled box yields "<old><new>": keep the new digit.
        if (digits[index] != null && raw.length == 2 && input.length == 2) input = input.takeLast(1)
        if (input.isEmpty()) {
            return copy(digits = digits.toMutableList().also { it[index] = null }, status = OtpStatus.None)
        }
        val next = digits.toMutableList()
        var last = index
        input.take(LENGTH - index).forEachIndexed { offset, c ->
            next[index + offset] = c
            last = index + offset
        }
        return copy(
            digits = next,
            focusedIndex = (last + 1).coerceAtMost(LENGTH - 1),
            status = OtpStatus.None,
        )
    }

    /** Backspace pressed in box [index]: clear it, or if already empty step back and clear the previous one. */
    fun onBackspace(index: Int): OtpState {
        if (index !in 0 until LENGTH) return this
        if (digits[index] != null) {
            return copy(digits = digits.toMutableList().also { it[index] = null }, focusedIndex = index, status = OtpStatus.None)
        }
        if (index == 0) return this
        return copy(
            digits = digits.toMutableList().also { it[index - 1] = null },
            focusedIndex = index - 1,
            status = OtpStatus.None,
        )
    }

    fun submit(): OtpState {
        if (!canSubmit) return this
        return if (code == VALID_CODE) {
            copy(status = OtpStatus.Success)
        } else {
            copy(digits = List(LENGTH) { null }, focusedIndex = 0, status = OtpStatus.Error)
        }
    }

    /** One second elapsed. */
    fun tick(): OtpState = if (resendSeconds > 0) copy(resendSeconds = resendSeconds - 1) else this

    /** Request a new code: only allowed when the countdown finished; restarts it. */
    fun resend(): OtpState =
        if (canResend) copy(resendSeconds = RESEND_SECONDS, status = OtpStatus.None) else this

    companion object {
        const val LENGTH = 6
        const val RESEND_SECONDS = 30
        const val VALID_CODE = "123456"
        const val SUCCESS_MESSAGE = "You're in."
        const val ERROR_MESSAGE = "That code didn't work. Try again."

        fun formatCountdown(seconds: Int): String {
            val s = seconds.coerceAtLeast(0)
            return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
        }
    }
}
