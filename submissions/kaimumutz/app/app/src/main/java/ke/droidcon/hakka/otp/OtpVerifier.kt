package ke.droidcon.hakka.otp

/** Checks a submitted code. A real app would call its backend here. */
fun interface OtpVerifier {
    fun verify(code: String): Boolean
}

/** Demo verifier for this challenge: accepts a single known code. */
class DemoOtpVerifier(private val acceptedCode: String = DEMO_CODE) : OtpVerifier {
    override fun verify(code: String): Boolean = code == acceptedCode

    companion object {
        const val DEMO_CODE = "123456"
    }
}
