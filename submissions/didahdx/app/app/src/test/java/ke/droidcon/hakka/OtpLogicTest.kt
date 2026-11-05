package ke.droidcon.hakka

import org.junit.Assert.*
import org.junit.Test

class OtpLogicTest {
    @Test fun typingAdvancesAndPreservesZero() {
        val result = OtpLogic.input(OtpState(), 0, "0")
        assertEquals("0", result.digits[0]); assertEquals(1, result.focus)
        assertFalse(result.complete)
    }
    @Test fun mixedPasteExtractsWholeCodeFromAnyBox() {
        val result = OtpLogic.input(OtpState(), 3, "Your code is 482913")
        assertEquals("482913", result.digits.joinToString("")); assertTrue(result.complete)
        assertEquals(5, result.focus)
    }
    @Test fun overflowIsTruncatedAndNonDigitsNeverPopulate() {
        assertEquals("123456", OtpLogic.input(OtpState(), 0, "123456789").digits.joinToString(""))
        assertEquals(OtpState(), OtpLogic.input(OtpState(), 0, "abc١２😊"))
    }
    @Test fun shortPasteStartsAtFocusedBoxAndClampsAtEnd() {
        val result = OtpLogic.input(OtpState(), 4, "9a87")
        assertEquals(listOf("", "", "", "", "9", "8"), result.digits)
        assertEquals(5, result.focus)
    }
    @Test fun deleteEmptyMovesBackAndClearsPreviousDigit() {
        val typed = OtpLogic.input(OtpState(), 0, "12")
        val deleted = OtpLogic.backspace(typed, 2)
        assertEquals(listOf("1", "", "", "", "", ""), deleted.digits)
        assertEquals(1, deleted.focus)
    }
    @Test fun deleteFilledClearsItAndFirstBoxCannotUnderflow() {
        val typed = OtpLogic.input(OtpState(), 0, "12")
        assertEquals(1, OtpLogic.backspace(typed, 1).focus)
        assertEquals("", OtpLogic.backspace(typed, 1).digits[1])
        assertEquals(0, OtpLogic.backspace(OtpState(), 0).focus)
    }
    @Test fun validCodeSucceedsAndIncompleteSubmitDoesNothing() {
        assertEquals(OtpState(), OtpLogic.submit(OtpState()))
        val result = OtpLogic.submit(OtpLogic.input(OtpState(), 0, "123456"))
        assertEquals(Verification.Success, result.verification)
        assertTrue(result.complete)
    }
    @Test fun wrongCodeClearsAndRequestsFirstFocusEvenIfAlreadyFocused() {
        val wrong = OtpLogic.input(OtpState(), 0, "000000").copy(focus = 0)
        val result = OtpLogic.submit(wrong)
        assertEquals(Verification.Error, result.verification)
        assertTrue(result.digits.all(String::isEmpty)); assertEquals(0, result.focus)
        assertTrue(result.focusRequest > wrong.focusRequest)
        assertEquals(Verification.Idle, OtpLogic.input(result, 0, "1").verification)
    }
    @Test fun countdownRoundsUpAndClampsAfterBackgroundDelay() {
        assertEquals(30, OtpLogic.remaining(30_000, 0))
        assertEquals(30, OtpLogic.remaining(30_000, 1))
        assertEquals(1, OtpLogic.remaining(30_000, 29_999))
        assertEquals(0, OtpLogic.remaining(30_000, 30_000))
        assertEquals(0, OtpLogic.remaining(30_000, 90_000))
    }
}
