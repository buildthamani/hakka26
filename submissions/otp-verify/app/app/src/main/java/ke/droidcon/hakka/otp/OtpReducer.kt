package ke.droidcon.hakka.otp

/**
 * Pure state transitions for the OTP screen. Keeping them free of Compose and Android
 * types makes the interaction rules easy to test and keeps [OtpViewModel] a thin adapter.
 */
object OtpReducer {

    fun fieldChanged(state: OtpUiState, index: Int, rawText: String): OtpUiState {
        if (index !in state.digits.indices) return state

        val sanitized = rawText.replace(OtpConfig.EMPTY_DIGIT_TEXT, "")
        val rawDigits = sanitized.filter(Char::isDigit)
        val previous = state.digits[index]

        if (rawDigits.isEmpty()) {
            if (sanitized.isNotEmpty()) return state // Non-digit input is rejected.
            return when {
                previous.isNotEmpty() -> clear(state, index)
                index > 0 -> backspace(state, index)
                else -> state
            }
        }

        // A filled box still contains its digit in the field text. Drop that leading
        // digit so typing replaces it and a full-code paste is read as six new digits.
        val digits = if (
            previous.isNotEmpty() &&
            rawDigits.length > 1 &&
            rawDigits.startsWith(previous) &&
            (rawDigits.length == 2 || rawDigits.length > OtpConfig.LENGTH)
        ) {
            rawDigits.drop(previous.length)
        } else {
            rawDigits
        }

        if (digits.isEmpty()) return state

        if (isPaste(digits, sanitized, previous)) {
            return paste(state, index, digits)
        }

        return state.copy(
            digits = state.digits.withDigit(index, digits.single().toString()),
            focusedIndex = nextIndex(index),
            status = OtpStatus.Idle,
        )
    }

    fun backspace(state: OtpUiState, index: Int): OtpUiState {
        val safeIndex = index.coerceIn(0, state.digits.lastIndex)
        return when {
            state.digits[safeIndex].isNotEmpty() -> clear(state, safeIndex)
            safeIndex > 0 -> state.copy(
                digits = state.digits.withDigit(safeIndex - 1, ""),
                focusedIndex = safeIndex - 1,
                status = OtpStatus.Idle,
            )
            else -> state.copy(status = OtpStatus.Idle)
        }
    }

    fun focus(state: OtpUiState, index: Int): OtpUiState =
        state.copy(focusedIndex = index.coerceIn(0, state.digits.lastIndex))

    fun tick(state: OtpUiState): OtpUiState =
        state.copy(secondsUntilResend = (state.secondsUntilResend - 1).coerceAtLeast(0))

    fun resend(state: OtpUiState): OtpUiState =
        if (!state.canResend) {
            state
        } else {
            state.copy(
                secondsUntilResend = OtpConfig.RESEND_SECONDS,
                focusedIndex = 0,
                status = OtpStatus.Idle,
            )
        }

    fun startVerifying(state: OtpUiState): OtpUiState =
        if (state.canSubmit) state.copy(isVerifying = true) else state

    fun applyVerificationResult(
        state: OtpUiState,
        result: OtpVerificationResult,
    ): OtpUiState = when (result) {
        OtpVerificationResult.Success -> state.copy(
            isVerifying = false,
            status = OtpStatus.Success,
        )
        OtpVerificationResult.Invalid -> state.copy(
            digits = List(OtpConfig.LENGTH) { "" },
            focusedIndex = 0,
            isVerifying = false,
            status = OtpStatus.Error,
        )
    }

    private fun clear(state: OtpUiState, index: Int): OtpUiState = state.copy(
        digits = state.digits.withDigit(index, ""),
        focusedIndex = index,
        status = OtpStatus.Idle,
    )

    private fun paste(
        state: OtpUiState,
        index: Int,
        pastedDigits: String,
    ): OtpUiState {
        val digits = pastedDigits.take(OtpConfig.LENGTH)
        if (digits.isEmpty()) return state

        // A full code always fills from the first box. A shorter paste fills from the
        // box that received it, matching how a user would paste into a specific slot.
        val start = if (digits.length == OtpConfig.LENGTH) {
            0
        } else {
            index.coerceAtMost(OtpConfig.LENGTH - digits.length)
        }
        val updated = state.digits.toMutableList()
        digits.forEachIndexed { offset, digit ->
            updated[start + offset] = digit.toString()
        }

        return state.copy(
            digits = updated,
            focusedIndex = nextIndex(start + digits.length - 1),
            status = OtpStatus.Idle,
        )
    }

    private fun isPaste(digits: String, sanitized: String, previous: String): Boolean =
        digits.length > 1 ||
            (digits.isNotEmpty() && sanitized.any { !it.isDigit() } && digits != previous)

    private fun nextIndex(index: Int): Int = (index + 1).coerceAtMost(OtpConfig.LENGTH - 1)

    private fun List<String>.withDigit(index: Int, value: String): List<String> =
        toMutableList().also { it[index] = value }
}
