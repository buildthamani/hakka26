package ke.droidcon.hakka

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpStateTest {

    private fun typed(code: String): OtpState =
        code.foldIndexed(OtpState()) { i, s, c -> s.input(i, c.toString()) }

    @Test
    fun startsEmptyFocusedOnFirstBox() {
        val s = OtpState()
        assertEquals(List(6) { "" }, s.digits)
        assertEquals(0, s.focusedIndex)
        assertFalse(s.canSubmit)
        assertEquals(OtpStatus.None, s.status)
    }

    @Test
    fun typingDigitFillsBoxAndAdvancesFocus() {
        val s = OtpState().input(0, "4")
        assertEquals("4", s.digits[0])
        assertEquals(1, s.focusedIndex)
        assertTrue(s.focusRequest > OtpState().focusRequest)
    }

    @Test
    fun nonDigitsAreIgnored() {
        val s = OtpState().input(0, "a")
        assertEquals("", s.digits[0])
        assertEquals(0, s.focusedIndex)
    }

    @Test
    fun typingOverFilledBoxKeepsOnlyNewDigit() {
        val s = OtpState().input(0, "3").input(0, "37")
        assertEquals("7", s.digits[0])
        assertEquals("7", OtpState().input(0, "3").input(0, "73").digits[0])
        assertEquals("", s.digits[1])
    }

    @Test
    fun lastBoxKeepsFocus() {
        val s = typed("12345").input(5, "6")
        assertEquals(5, s.focusedIndex)
        assertTrue(s.isComplete)
    }

    @Test
    fun backspaceOnEmptyBoxMovesBackAndClearsPrevious() {
        val s = typed("12").backspace(2)
        assertEquals(listOf("1", "", "", "", "", ""), s.digits)
        assertEquals(1, s.focusedIndex)
    }

    @Test
    fun backspaceOnFirstEmptyBoxDoesNothing() {
        assertEquals(OtpState(), OtpState().backspace(0))
    }

    @Test
    fun deletingDigitClearsBoxInPlace() {
        val s = typed("12").input(1, "")
        assertEquals(listOf("1", "", "", "", "", ""), s.digits)
    }

    @Test
    fun pasteExtractsDigitsFromText() {
        val s = OtpState().paste("Your code is 482913")
        assertEquals("482913", s.code)
        assertTrue(s.canSubmit)
        assertEquals(5, s.focusedIndex)
    }

    @Test
    fun pasteIntoAnyBoxViaInputFillsAllSix() {
        val s = OtpState().input(3, "Your code is 482913")
        assertEquals("482913", s.code)
    }

    @Test
    fun extractCodePrefersSixDigitRun() {
        assertEquals("482913", OtpState.extractCode("Ref 12: Your code is 482913"))
        assertEquals("482913", OtpState.extractCode("48-29-13"))
        assertEquals("12", OtpState.extractCode("ab12"))
    }

    @Test
    fun partialPasteFillsFromCurrentBox() {
        val s = OtpState().input(0, "9").paste("12", index = 1)
        assertEquals(listOf("9", "1", "2", "", "", ""), s.digits)
        assertEquals(3, s.focusedIndex)
    }

    @Test
    fun submitDisabledUntilComplete() {
        val partial = typed("12345")
        assertFalse(partial.canSubmit)
        assertEquals(partial, partial.submit())
    }

    @Test
    fun correctCodeSucceeds() {
        val s = typed("123456").submit()
        assertEquals(OtpStatus.Success, s.status)
        assertEquals("You're in.", s.statusMessage)
    }

    @Test
    fun wrongCodeShowsErrorClearsBoxesAndRefocusesFirst() {
        val before = typed("654321")
        val s = before.submit()
        assertEquals(OtpStatus.Error, s.status)
        assertEquals("That code didn't work. Try again.", s.statusMessage)
        assertEquals(List(6) { "" }, s.digits)
        assertEquals(0, s.focusedIndex)
        assertNotEquals(before.focusRequest, s.focusRequest)
    }

    @Test
    fun typingAfterErrorClearsStatus() {
        val s = typed("000000").submit().input(0, "1")
        assertEquals(OtpStatus.None, s.status)
    }

    @Test
    fun resendCountsDownFromThirty() {
        var s = OtpState()
        assertEquals(30, s.resendSecondsLeft)
        assertFalse(s.canResend)
        assertEquals("Resend in 0:30", s.resendLabel)
        repeat(21) { s = s.tick() }
        assertEquals("Resend in 0:09", s.resendLabel)
        repeat(20) { s = s.tick() }
        assertEquals(0, s.resendSecondsLeft)
        assertTrue(s.canResend)
        assertEquals("Didn't receive the code? Resend", s.resendLabel)
    }

    @Test
    fun resendIgnoredDuringCountdown() {
        val s = OtpState().tick()
        assertEquals(s, s.resend())
    }

    @Test
    fun resendRestartsCountdown() {
        var s = OtpState()
        repeat(30) { s = s.tick() }
        val r = s.resend()
        assertEquals(30, r.resendSecondsLeft)
        assertFalse(r.canResend)
        assertEquals(s.resendCount + 1, r.resendCount)
    }

    @Test
    fun digitLabelsForScreenReaders() {
        assertEquals("Digit 1 of 6", OtpCopy.digitLabel(0))
        assertEquals("Digit 6 of 6", OtpCopy.digitLabel(5))
    }
}
