package ke.droidcon.hakka.otp

import ke.droidcon.hakka.otp.OtpState.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpStateTest {

    private fun typed(code: String): OtpState =
        code.foldIndexed(OtpState()) { i, s, c -> s.input(i, c.toString()) }

    @Test
    fun startsEmptyWithFocusOnFirstBoxAndNoRealZeros() {
        val s = OtpState()
        assertTrue(s.digits.all { it == null })
        assertEquals("", s.code)
        assertEquals(0, s.focusedIndex)
        assertEquals(Status.Idle, s.status)
        assertFalse(s.canSubmit)
    }

    @Test
    fun typingADigitFillsBoxAndAdvancesFocus() {
        val s = OtpState().input(0, "4")
        assertEquals('4', s.digits[0])
        assertEquals(1, s.focusedIndex)
    }

    @Test
    fun focusStaysOnLastBoxAfterFillingIt() {
        assertEquals(5, typed("123456").focusedIndex)
    }

    @Test
    fun nonDigitInputIsIgnored() {
        val s = OtpState()
        assertSame(s, s.input(0, "a"))
        assertSame(s, s.input(0, " -."))
    }

    @Test
    fun backspaceOnFilledBoxClearsItAndKeepsFocus() {
        val s = typed("12").focus(1).backspace(1)
        assertNull(s.digits[1])
        assertEquals('1', s.digits[0])
        assertEquals(1, s.focusedIndex)
    }

    @Test
    fun backspaceOnEmptyBoxMovesBackAndClearsPreviousDigit() {
        val s = typed("123")
        assertEquals(3, s.focusedIndex)
        val after = s.backspace(3)
        assertEquals(2, after.focusedIndex)
        assertNull(after.digits[2])
        assertEquals("12", after.code)
    }

    @Test
    fun backspaceOnFirstEmptyBoxDoesNothing() {
        val s = OtpState().backspace(0)
        assertEquals(0, s.focusedIndex)
        assertEquals("", s.code)
    }

    @Test
    fun pastingSentenceExtractsDigitsIntoAllBoxes() {
        val s = OtpState().paste("Your code is 482913")
        assertEquals("482913", s.code)
        assertTrue(s.isComplete)
        assertEquals(5, s.focusedIndex)
    }

    @Test
    fun multiDigitInputIntoAnyBoxIsTreatedAsPaste() {
        val s = OtpState().input(3, "Your code is 482913")
        assertEquals("482913", s.code)
    }

    @Test
    fun shortPasteFillsFromCurrentBox() {
        val s = OtpState().input(2, "78")
        assertEquals(listOf(null, null, '7', '8', null, null), s.digits)
        assertEquals(0, s.focusedIndex) // first empty box
    }

    @Test
    fun pasteKeepsOnlyFirstSixDigits() {
        assertEquals("123456", OtpState().paste("12345678").code)
    }

    @Test
    fun extractDigits() {
        assertEquals("482913", OtpState.extractDigits("Your code is 482913"))
        assertEquals("", OtpState.extractDigits("no digits"))
    }

    @Test
    fun submitDisabledUntilAllSixFilled() {
        val five = typed("12345")
        assertFalse(five.canSubmit)
        assertSame(five, five.submit())
        assertTrue(typed("123456").canSubmit)
    }

    @Test
    fun correctCodeSucceeds() {
        val s = typed("123456").submit()
        assertEquals(Status.Success, s.status)
        assertEquals("123456", s.code)
    }

    @Test
    fun wrongCodeShowsErrorClearsBoxesAndRefocusesFirst() {
        val s = typed("654321").submit()
        assertEquals(Status.Error, s.status)
        assertTrue(s.digits.all { it == null })
        assertEquals(0, s.focusedIndex)
        assertFalse(s.canSubmit)
    }

    @Test
    fun typingAfterErrorResetsStatus() {
        val s = typed("000000").submit().input(0, "1")
        assertEquals(Status.Idle, s.status)
    }

    @Test
    fun resendDisabledDuringThirtySecondCountdown() {
        var s = OtpState()
        assertEquals(30, s.resendSecondsLeft)
        assertEquals("0:30", s.countdownLabel)
        assertFalse(s.canResend)
        assertSame(s, s.resend())
        repeat(29) { s = s.tick() }
        assertEquals("0:01", s.countdownLabel)
        assertFalse(s.canResend)
        s = s.tick()
        assertTrue(s.canResend)
        assertEquals(0, s.tick().resendSecondsLeft)
    }

    @Test
    fun resendRestartsCountdown() {
        var s = OtpState()
        repeat(30) { s = s.tick() }
        val restarted = s.resend()
        assertEquals(30, restarted.resendSecondsLeft)
        assertFalse(restarted.canResend)
        assertEquals(s.countdownGeneration + 1, restarted.countdownGeneration)
    }

    @Test
    fun focusIgnoresOutOfRange() {
        val s = OtpState()
        assertSame(s, s.focus(6))
        assertSame(s, s.focus(-1))
        assertEquals(3, s.focus(3).focusedIndex)
    }
}
