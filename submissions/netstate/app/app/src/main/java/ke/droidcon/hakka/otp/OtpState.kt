package ke.droidcon.hakka.otp

enum class OtpStatus {
    Idle, Success, Error
}

data class OtpState(
    val digits: String = "",
    val secondsLeft: Int = 30,
    val status: OtpStatus = OtpStatus.Idle
) {
    val activeIndex: Int
        get() = digits.length.coerceAtMost(5)

    val isSubmitEnabled: Boolean
        get() = digits.length == 6

    val canResend: Boolean
        get() = secondsLeft == 0
}
