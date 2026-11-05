package ke.droidcon.hakka

import ke.droidcon.hakka.domain.usecase.ExtractOtpDigitsUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtractOtpDigitsUseCaseTest {

    private val extractOtpDigitsUseCase = ExtractOtpDigitsUseCase()

    @Test
    fun extractsDigitsFromSentence() {
        assertEquals("482913", extractOtpDigitsUseCase("Your code is 482913"))
    }

    @Test
    fun stripsNonDigits() {
        assertEquals("123456", extractOtpDigitsUseCase("code: 1-2-3-4-5-6!"))
    }

    @Test
    fun capsAtSixDigits() {
        assertEquals("123456", extractOtpDigitsUseCase("1234567890"))
    }

    @Test
    fun handlesEmptyAndNoDigits() {
        assertEquals("", extractOtpDigitsUseCase("No digits here"))
        assertEquals("", extractOtpDigitsUseCase(""))
    }
}
