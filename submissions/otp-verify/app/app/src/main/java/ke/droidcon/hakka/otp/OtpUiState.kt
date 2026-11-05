package ke.droidcon.hakka.otp

enum class OtpStatus { Idle, Success, Error }

/**
 * Single source of truth for the OTP screen. Everything the UI renders comes from here.
 */
data class OtpUiState(
    val digits: List<String> = List(OtpConfig.LENGTH) { "" },
    val focusedIndex: Int = 0,
    val status: OtpStatus = OtpStatus.Idle,
    val secondsUntilResend: Int = OtpConfig.RESEND_SECONDS,
    val isVerifying: Boolean = false,
) {
    val code: String get() = digits.joinToString(separator = "")

    val isComplete: Boolean
        get() = digits.size == OtpConfig.LENGTH && digits.all { it.isNotEmpty() }

    val canSubmit: Boolean get() = isComplete && !isVerifying

    val canResend: Boolean get() = secondsUntilResend <= 0 && !isVerifying
}
