package ke.droidcon.hakka.otp

/**
 * Pure-Kotlin, immutable model of the OTP screen. Every operation returns a new state,
 * so the UI just holds the latest value and the logic is trivially unit-testable.
 *
 * Empty boxes are stored as `null`; the grey "0" the UI shows is only a placeholder.
 */
data class OtpState(
    val digits: List<Char?> = List(LENGTH) { null },
    val focusedIndex: Int = 0,
    val status: Status = Status.Idle,
    val resendSecondsLeft: Int = RESEND_SECONDS,
    /** Bumped every time the countdown (re)starts, so the UI can restart its ticker. */
    val countdownGeneration: Int = 0,
) {
    enum class Status { Idle, Success, Error }

    init {
        require(digits.size == LENGTH) { "Expected $LENGTH digits" }
        require(digits.all { it == null || it in '0'..'9' }) { "Digits only" }
    }

    val code: String get() = digits.joinToString("") { it?.toString().orEmpty() }
    val isComplete: Boolean get() = digits.all { it != null }
    val canSubmit: Boolean get() = isComplete
    val canResend: Boolean get() = resendSecondsLeft == 0

    /** "0:SS" label for the countdown. */
    val countdownLabel: String
        get() = "${resendSecondsLeft / 60}:${(resendSecondsLeft % 60).toString().padStart(2, '0')}"

    /**
     * Text arrived in box [index]. A single digit fills that box and advances focus.
     * Anything with more than one digit (a paste such as "Your code is 482913") is treated
     * as a paste: six or more digits fill every box, fewer fill from [index] onwards.
     * Text without digits is ignored.
     */
    fun input(index: Int, text: String): OtpState {
        val incoming = extractDigits(text)
        if (incoming.isEmpty() || index !in 0 until LENGTH) return this
        if (incoming.length == 1) {
            val updated = digits.toMutableList().also { it[index] = incoming[0] }
            return copy(
                digits = updated,
                focusedIndex = (index + 1).coerceAtMost(LENGTH - 1),
                status = Status.Idle,
            )
        }
        return paste(text, if (incoming.length >= LENGTH) 0 else index)
    }

    /** Extracts digits from arbitrary text and fills boxes starting at [start]. */
    fun paste(text: String, start: Int = 0): OtpState {
        val incoming = extractDigits(text)
        if (incoming.isEmpty()) return this
        val updated = digits.toMutableList()
        var last = start
        incoming.take(LENGTH - start).forEachIndexed { offset, c ->
            last = start + offset
            updated[last] = c
        }
        val nextEmpty = updated.indexOfFirst { it == null }
        return copy(
            digits = updated,
            focusedIndex = if (nextEmpty >= 0) nextEmpty else LENGTH - 1,
            status = Status.Idle,
        )
    }

    /**
     * Backspace in box [index]: a filled box is cleared and keeps focus; an empty box moves
     * focus back and clears the previous digit.
     */
    fun backspace(index: Int): OtpState {
        if (index !in 0 until LENGTH) return this
        val updated = digits.toMutableList()
        return if (updated[index] != null) {
            updated[index] = null
            copy(digits = updated, focusedIndex = index, status = Status.Idle)
        } else if (index > 0) {
            updated[index - 1] = null
            copy(digits = updated, focusedIndex = index - 1, status = Status.Idle)
        } else {
            copy(focusedIndex = 0)
        }
    }

    fun focus(index: Int): OtpState =
        if (index in 0 until LENGTH && index != focusedIndex) copy(focusedIndex = index) else this

    /** Verifies the code. Wrong codes clear every box and return focus to the first. */
    fun submit(): OtpState {
        if (!canSubmit) return this
        return if (code == VALID_CODE) {
            copy(status = Status.Success)
        } else {
            copy(digits = List(LENGTH) { null }, focusedIndex = 0, status = Status.Error)
        }
    }

    /** One second of the resend countdown has elapsed. */
    fun tick(): OtpState =
        if (resendSecondsLeft > 0) copy(resendSecondsLeft = resendSecondsLeft - 1) else this

    /** Restarts the 30 s countdown; a no-op while the countdown is still running. */
    fun resend(): OtpState =
        if (!canResend) this
        else copy(resendSecondsLeft = RESEND_SECONDS, countdownGeneration = countdownGeneration + 1)

    companion object {
        const val LENGTH = 6
        const val RESEND_SECONDS = 30
        const val VALID_CODE = "123456"

        fun extractDigits(text: String): String = text.filter { it in '0'..'9' }
    }
}
