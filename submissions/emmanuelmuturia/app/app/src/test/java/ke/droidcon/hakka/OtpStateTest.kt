package ke.droidcon.hakka

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpStateTest {
    private fun typed(s: String): OtpState =
        s.fold(OtpState()) { st, c -> st.onInput(st.focusedIndex, c.toString()) }

    @Test fun typingAdvancesFocus() {
        val s = OtpState().onInput(0, "1")
        assertEquals('1', s.digits[0])
        assertEquals(1, s.focusedIndex)
    }

    @Test fun lastBoxKeepsFocus() {
        val s = typed("123456")
        assertEquals(5, s.focusedIndex)
        assertTrue(s.isComplete)
    }

    @Test fun nonDigitsIgnored() {
        val s = OtpState().onInput(0, "a")
        assertNull(s.digits[0])
        assertEquals(0, s.focusedIndex)
    }

    @Test fun pasteExtractsDigitsAndFills() {
        val s = OtpState().onInput(0, "12-34 56")
        assertEquals("123456", s.code)
    }

    @Test fun pasteFromMiddleTruncates() {
        val s = OtpState().onInput(4, "9876")
        assertEquals("98", s.code)
        assertEquals(5, s.focusedIndex)
    }

    @Test fun typingIntoFilledBoxReplaces() {
        val s = typed("1").focus(0).onInput(0, "17")
        assertEquals('7', s.digits[0])
    }

    @Test fun backspaceClearsCurrentThenMovesBack() {
        val s = typed("12")           // focus on box 2 (empty)
        val b1 = s.onBackspace(2)
        assertEquals(1, b1.focusedIndex)
        assertNull(b1.digits[1])
        val b2 = b1.focus(0).onBackspace(0)
        assertNull(b2.digits[0])
    }

    @Test fun backspaceOnFilledBoxClearsInPlace() {
        val s = typed("12").onBackspace(1)
        assertNull(s.digits[1])
        assertEquals(1, s.focusedIndex)
        assertEquals('1', s.digits[0])
    }

    @Test fun submitDisabledUntilSixDigits() {
        assertFalse(typed("12345").canSubmit)
        assertTrue(typed("123456").canSubmit)
        assertEquals(OtpStatus.None, typed("12345").submit().status)
    }

    @Test fun correctCodeSucceeds() {
        val s = typed("123456").submit()
        assertEquals(OtpStatus.Success, s.status)
        assertEquals("You're in.", s.statusMessage)
    }

    @Test fun wrongCodeClearsAndFocusesFirst() {
        val s = typed("111111").submit()
        assertEquals(OtpStatus.Error, s.status)
        assertEquals("That code didn't work. Try again.", s.statusMessage)
        assertEquals("", s.code)
        assertEquals(0, s.focusedIndex)
    }

    @Test fun editingClearsError() {
        val s = typed("111111").submit().onInput(0, "1")
        assertEquals(OtpStatus.None, s.status)
    }

    @Test fun countdownAndResend() {
        var s = OtpState()
        assertEquals("Resend in 0:30", s.resendLabel)
        assertFalse(s.canResend)
        assertEquals(s, s.resend())
        repeat(21) { s = s.tick() }
        assertEquals("Resend in 0:09", s.resendLabel)
        repeat(9) { s = s.tick() }
        assertTrue(s.canResend)
        assertEquals("Resend", s.resendLabel)
        assertEquals(0, s.tick().resendSeconds)
        assertEquals(30, s.resend().resendSeconds)
    }
}
