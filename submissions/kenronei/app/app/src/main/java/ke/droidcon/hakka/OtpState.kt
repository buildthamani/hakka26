package ke.droidcon.hakka

/** Exact on-screen copy from issue #1. */
object OtpCopy {
    const val TITLE = "Enter your code"
    const val SUBTITLE = "A 6-digit code has been sent to your phone."
    const val RESEND_PROMPT = "Didn't receive the code? "
    const val RESEND = "Resend"
    const val SUBMIT = "Submit"
    const val SUCCESS = "You're in."
    const val ERROR = "That code didn't work. Try again."
    const val PLACEHOLDER = "-"

    fun resendCountdown(seconds: Int): String = "Resend in 0:%02d".format(java.util.Locale.ROOT, seconds.coerceIn(0, 59))
    fun digitLabel(index: Int): String = "Digit ${index + 1} of ${OtpState.LENGTH}"
}

enum class OtpStatus { None, Success, Error }

/**
 * Immutable, pure-Kotlin model of the OTP screen. Every transition returns a new state,
 * so callers always derive the next state from the latest one (`state = state.tick()`).
 */
data class OtpState(
    val digits: List<String> = List(LENGTH) { "" },
    val focusedIndex: Int = 0,
    /** Bumped whenever the model (not the user) moves focus, so the UI re-requests it. */
    val focusRequest: Int = 0,
    val status: OtpStatus = OtpStatus.None,
    val resendSecondsLeft: Int = RESEND_SECONDS,
    /** Bumped on every resend so the UI restarts its countdown ticker. */
    val resendCount: Int = 0,
) {
    init {
        require(digits.size == LENGTH) { "Expected $LENGTH digits" }
    }

    val code: String get() = digits.joinToString("")
    val isComplete: Boolean get() = digits.all { it.length == 1 }
    val canSubmit: Boolean get() = isComplete
    val canResend: Boolean get() = resendSecondsLeft == 0
    val statusMessage: String
        get() = when (status) {
            OtpStatus.None -> ""
            OtpStatus.Success -> OtpCopy.SUCCESS
            OtpStatus.Error -> OtpCopy.ERROR
        }
    val resendLabel: String
        get() = if (canResend) OtpCopy.RESEND_PROMPT + OtpCopy.RESEND else OtpCopy.resendCountdown(resendSecondsLeft)

    /**
     * Handles a raw text change in box [index]. [previous] is what the box held before.
     * A single new digit fills the box and advances; multi-character input is treated as a paste.
     */
    fun input(index: Int, raw: String): OtpState {
        if (index !in 0 until LENGTH) return this
        val previous = digits[index]
        // A box holding one digit that receives one more character: keep only the new one,
        // whichever side of the cursor it landed on.
        val added = if (previous.isNotEmpty() && raw.length == previous.length + 1 && raw.contains(previous)) {
            raw.replaceFirst(previous, "")
        } else {
            raw
        }
        val newDigits = added.filter { it.isDigit() }
        return when {
            raw.isEmpty() -> clear(index)
            added.length > 1 || newDigits.length > 1 -> paste(raw, index)
            newDigits.isEmpty() -> this // non-digit typed: ignore
            else -> {
                val next = digits.toMutableList().also { it[index] = newDigits }
                copy(
                    digits = next,
                    status = OtpStatus.None,
                ).focus((index + 1).coerceAtMost(LENGTH - 1))
            }
        }
    }

    /** Extracts digits from pasted text such as "Your code is 482913". */
    fun paste(text: String, index: Int = 0): OtpState {
        val code = extractCode(text)
        if (code.isEmpty()) return this
        val start = if (code.length == LENGTH) 0 else index.coerceIn(0, LENGTH - 1)
        val next = digits.toMutableList()
        code.take(LENGTH - start).forEachIndexed { i, c -> next[start + i] = c.toString() }
        val lastFilled = start + code.length.coerceAtMost(LENGTH - start) - 1
        return copy(digits = next, status = OtpStatus.None)
            .focus((lastFilled + 1).coerceAtMost(LENGTH - 1))
    }

    /** Backspace pressed while box [index] is empty: move back and clear the previous digit. */
    fun backspace(index: Int): OtpState {
        if (index !in 0 until LENGTH) return this
        if (digits[index].isNotEmpty()) return clear(index)
        if (index == 0) return this
        val next = digits.toMutableList().also { it[index - 1] = "" }
        return copy(digits = next).focus(index - 1)
    }

    fun clear(index: Int): OtpState {
        if (index !in 0 until LENGTH) return this
        val next = digits.toMutableList().also { it[index] = "" }
        return copy(digits = next)
    }

    /** The user moved focus themselves (e.g. tapped a box). */
    fun onFocused(index: Int): OtpState =
        if (index in 0 until LENGTH && index != focusedIndex) copy(focusedIndex = index) else this

    private fun focus(index: Int): OtpState = copy(focusedIndex = index, focusRequest = focusRequest + 1)

    fun submit(): OtpState {
        if (!canSubmit) return this
        return if (code == VALID_CODE) {
            copy(status = OtpStatus.Success)
        } else {
            copy(digits = List(LENGTH) { "" }, status = OtpStatus.Error).focus(0)
        }
    }

    fun tick(): OtpState =
        if (resendSecondsLeft > 0) copy(resendSecondsLeft = resendSecondsLeft - 1) else this

    fun resend(): OtpState =
        if (canResend) copy(resendSecondsLeft = RESEND_SECONDS, resendCount = resendCount + 1) else this

    companion object {
        const val LENGTH = 6
        const val RESEND_SECONDS = 30
        const val VALID_CODE = "123456"

        /** Prefers a run of six consecutive digits; otherwise all digits in order (max six). */
        fun extractCode(text: String): String =
            Regex("\\d{$LENGTH}").find(text)?.value ?: text.filter { it.isDigit() }.take(LENGTH)
    }
}
