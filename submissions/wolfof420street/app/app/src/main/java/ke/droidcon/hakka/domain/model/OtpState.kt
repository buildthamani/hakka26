package ke.droidcon.hakka.domain.model

const val OTP_LENGTH = 6
const val RESEND_SECONDS = 30
const val VALID_CODE = "123456"

data class OtpState(
    val digits: List<String> = List(OTP_LENGTH) { "" },
    val focusedIndex: Int = 0,
    val status: OtpStatus = OtpStatus.Idle,
    val resendSecondsLeft: Int = RESEND_SECONDS,
) {
    val code: String get() = digits.joinToString("")
    val isComplete: Boolean get() = digits.all { it.isNotEmpty() }
    val canResend: Boolean get() = resendSecondsLeft == 0
}
