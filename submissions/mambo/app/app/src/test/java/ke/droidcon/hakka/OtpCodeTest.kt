package ke.droidcon.hakka

import org.junit.Assert.assertEquals
import org.junit.Test

class OtpCodeTest {
    @Test
    fun correctCodeLetsYouIn() {
        assertEquals("You're in.", checkCode("123456"))
    }

    @Test
    fun wrongCodeShowsError() {
        assertEquals("That code didn't work. Try again.", checkCode("000000"))
    }
}
