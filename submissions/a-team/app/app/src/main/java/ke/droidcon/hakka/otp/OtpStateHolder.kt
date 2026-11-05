package ke.droidcon.hakka.otp

/** The code the challenge accepts. There is no backend, so verification is local. */
const val DEMO_VALID_CODE = "123456"

/**
 * Pure-Kotlin owner of all OTP screen logic. It has no Android or coroutine dependencies, so it
 * is driven directly from unit tests; the UI forwards events here and re-renders on [onStateChanged].
 * The countdown is advanced by calling [onTick] once per second (a `LaunchedEffect` does that in the UI).
 */
class OtpStateHolder(
    private val verifyCode: (String) -> Boolean = { it == DEMO_VALID_CODE },
    initialState: OtpUiState = OtpUiState(),
    private val onStateChanged: (OtpUiState) -> Unit = {},
) {
    var state: OtpUiState = initialState
        private set(value) {
            if (field != value) {
                field = value
                onStateChanged(value)
            }
        }

    private val lastIndex = OTP_LENGTH - 1

    fun onFocus(index: Int) {
        if (index in 0..lastIndex) state = state.copy(focusedIndex = index)
    }

    /**
     * Handles the raw text a box's text field reports. A single new digit is entered and focus
     * advances; several characters are treated as a paste; non-digits are ignored.
     */
    fun onInput(index: Int, rawText: String) {
        if (index !in 0..lastIndex) return
        if (rawText.isEmpty()) {
            updateDigits(focusedIndex = index) { it[index] = null }
            return
        }
        val current = state.digits[index]?.toString().orEmpty()
        val inserted = when {
            current.isEmpty() -> rawText
            rawText.startsWith(current) -> rawText.removePrefix(current)
            rawText.endsWith(current) -> rawText.removeSuffix(current)
            else -> rawText
        }
        val insertedDigits = inserted.filter(Char::isDigit)
        when {
            insertedDigits.isEmpty() -> Unit // non-digits are rejected
            insertedDigits.length == 1 -> updateDigits(focusedIndex = (index + 1).coerceAtMost(lastIndex)) {
                it[index] = insertedDigits.single()
            }
            else -> onPaste(inserted, startIndex = index)
        }
    }

    /**
     * Extracts the digits from pasted text such as "Your code is 482913". A full code fills every
     * box from the start; a shorter fragment fills from [startIndex].
     */
    fun onPaste(text: String, startIndex: Int = 0) {
        val pasted = text.filter(Char::isDigit)
        if (pasted.isEmpty()) return
        val start = if (pasted.length >= OTP_LENGTH) 0 else startIndex.coerceIn(0, lastIndex)
        val toFill = pasted.take(OTP_LENGTH - start)
        updateDigits(focusedIndex = (start + toFill.length).coerceAtMost(lastIndex)) { digits ->
            toFill.forEachIndexed { offset, digit -> digits[start + offset] = digit }
        }
    }

    /** Backspace on a filled box clears it; on an empty box it moves back and clears the previous one. */
    fun onBackspace(index: Int) {
        if (index !in 0..lastIndex) return
        when {
            state.digits[index] != null -> updateDigits(focusedIndex = index) { it[index] = null }
            index > 0 -> updateDigits(focusedIndex = index - 1) { it[index - 1] = null }
        }
    }

    fun onSubmit() {
        if (!state.canSubmit) return
        state = if (verifyCode(state.code)) {
            state.copy(status = OtpStatus.Success)
        } else {
            state.copy(
                digits = List(OTP_LENGTH) { null },
                focusedIndex = 0,
                status = OtpStatus.Error,
            )
        }
    }

    fun onTick() {
        if (state.isCountingDown) state = state.copy(resendSecondsLeft = state.resendSecondsLeft - 1)
    }

    fun onResend() {
        if (state.canResend) state = state.copy(resendSecondsLeft = RESEND_COUNTDOWN_SECONDS)
    }

    private inline fun updateDigits(focusedIndex: Int, edit: (MutableList<Char?>) -> Unit) {
        val digits = state.digits.toMutableList().also(edit)
        state = state.copy(digits = digits, focusedIndex = focusedIndex, status = OtpStatus.Idle)
    }
}
