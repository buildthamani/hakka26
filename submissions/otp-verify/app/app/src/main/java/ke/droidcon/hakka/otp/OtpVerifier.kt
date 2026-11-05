package ke.droidcon.hakka.otp

enum class OtpVerificationResult { Success, Invalid }

/**
 * Boundary between the screen and whatever actually checks a code. A network-backed
 * implementation can replace [LocalOtpVerifier] without touching the UI or [OtpViewModel].
 */
interface OtpVerifier {
    suspend fun verify(code: String): OtpVerificationResult
}

/**
 * The screen must accept 123456 for the challenge. The accepted code is a constructor
 * argument rather than a branch hidden inside the UI.
 */
class LocalOtpVerifier(
    private val validCode: String = OtpConfig.VALID_CODE,
) : OtpVerifier {
    override suspend fun verify(code: String): OtpVerificationResult =
        if (code.length == OtpConfig.LENGTH && code == validCode) {
            OtpVerificationResult.Success
        } else {
            OtpVerificationResult.Invalid
        }
}
