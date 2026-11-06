package ke.droidcon.hakka

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpStateTest {
    @Test
    fun incompleteCodeCannotBeSubmitted() {
        val state = OtpState()

        assertFalse(state.complete)
        assertEquals(state, state.submit())
    }

    @Test
    fun enteringDigitAdvancesFocus() {
        val state = OtpState().input(index = 0, raw = "4")

        assertEquals("4", state.digits[0])
        assertEquals(1, state.focus)
    }

    @Test
    fun pastedMessageFillsCodeFromFirstBox() {
        val state = OtpState().input(index = 3, raw = "Your code is 482913")

        assertEquals("482913", state.digits.joinToString(""))
        assertTrue(state.complete)
        assertEquals(5, state.focus)
    }

    @Test
    fun inputIgnoresNonDigitsAndTruncatesLongCodes() {
        val initial = OtpState()
        val longCode = initial.input(index = 0, raw = "12345678")

        assertEquals(initial, initial.input(index = 0, raw = "abc!"))
        assertEquals("123456", longCode.digits.joinToString(""))
    }

    @Test
    fun backspaceOnEmptyBoxClearsPreviousDigit() {
        val state = OtpState().input(index = 0, raw = "4").backspace(index = 1)

        assertEquals("", state.digits[0])
        assertEquals(0, state.focus)
        assertEquals(0, OtpState().backspace(index = 0).focus)
    }

    @Test
    fun backspaceOnFilledBoxClearsItWithoutMovingFocus() {
        val state = OtpState().input(index = 0, raw = "12").backspace(index = 1)

        assertEquals("1", state.digits[0])
        assertEquals("", state.digits[1])
        assertEquals(1, state.focus)
    }

    @Test
    fun correctCodeShowsSuccess() {
        val state = OtpState().input(index = 0, raw = "123456").submit()

        assertEquals("You're in.", state.status)
    }

    @Test
    fun wrongCodeClearsDigitsAndErrorDisappearsOnRetry() {
        val failed = OtpState().input(index = 0, raw = "482913").submit()
        val retry = failed.input(index = 0, raw = "1")

        assertEquals("That code didn't work. Try again.", failed.status)
        assertEquals(List(6) { "" }, failed.digits)
        assertEquals(0, failed.focus)
        assertFalse(failed.complete)
        assertEquals("", retry.status)
    }

    @Test
    fun countdownRoundsUpAndStopsAtZero() {
        assertEquals(30, remainingSeconds(deadline = 30_000, now = 0))
        assertEquals(1, remainingSeconds(deadline = 30_000, now = 29_999))
        assertEquals(0, remainingSeconds(deadline = 30_000, now = 30_000))
        assertEquals(0, remainingSeconds(deadline = 30_000, now = 40_000))
        assertEquals(30, remainingSeconds(deadline = 70_000, now = 40_000))
    }
}
