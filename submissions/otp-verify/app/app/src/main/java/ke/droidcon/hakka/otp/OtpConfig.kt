package ke.droidcon.hakka.otp

/**
 * Fixed OTP screen configuration. The code that must be accepted is injected into the
 * verifier, so this object only carries the screen's structural constants.
 */
object OtpConfig {
    const val LENGTH = 6
    const val RESEND_SECONDS = 30
    const val VALID_CODE = "123456"

    /**
     * A zero-width space keeps an empty [androidx.compose.foundation.text.BasicTextField]
     * editable, so the IME reports backspace even when the visible box has no digit.
     */
    const val EMPTY_DIGIT_TEXT = "\u200B"
}
