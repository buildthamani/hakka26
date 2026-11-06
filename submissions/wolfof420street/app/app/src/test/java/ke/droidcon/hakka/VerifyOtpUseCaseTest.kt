package ke.droidcon.hakka

import ke.droidcon.hakka.domain.usecase.VerifyOtpUseCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VerifyOtpUseCaseTest {

    private val verifyOtpUseCase = VerifyOtpUseCase()

    @Test
    fun validCodeReturnsTrue() {
        assertTrue(verifyOtpUseCase("123456"))
    }

    @Test
    fun wrongCodeReturnsFalse() {
        assertFalse(verifyOtpUseCase("654321"))
        assertFalse(verifyOtpUseCase("000000"))
        assertFalse(verifyOtpUseCase("12345"))
        assertFalse(verifyOtpUseCase(""))
    }
}
