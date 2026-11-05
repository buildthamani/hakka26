package ke.droidcon.hakka.domain

/** Decides whether a complete one-time code is valid. */
fun interface OtpVerifier {
    fun verify(code: String): Boolean
}

/** Demo verifier: only [validCode] is accepted. */
class FixedOtpVerifier(private val validCode: String = "123456") : OtpVerifier {
    override fun verify(code: String): Boolean = code == validCode
}
