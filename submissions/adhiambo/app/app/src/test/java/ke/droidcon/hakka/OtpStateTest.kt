package ke.droidcon.hakka

import org.junit.Assert.*
import org.junit.Test

class OtpStateTest {
    @Test fun initialStateStartsThirtySecondCountdown() {
        val state = OtpState.initial(1000)
        assertEquals(List(6) { "" }, state.digits)
        assertEquals(0, state.active)
        assertFalse(state.complete)
        assertEquals(30, state.secondsRemaining(1000))
    }
    @Test fun inputAdvancesAndPreservesLeadingZero() {
        val state = OtpState.initial(0).input(0, "0")
        assertEquals("0", state.digits[0])
        assertEquals(1, state.active)
    }
    @Test fun mixedPasteExtractsSixDigits() {
        val state = OtpState.initial(0).input(0, "Your code is 482913")
        assertEquals("482913", state.digits.joinToString(""))
        assertEquals(5, state.active)
        assertTrue(state.complete)
    }
    @Test fun pasteDropsOverflowAndIgnoresNonNumericInput() {
        val initial = OtpState.initial(0)
        assertEquals(initial, initial.input(0, "abc-١"))
        assertEquals("123456", initial.input(0, "123456789").digits.joinToString(""))
    }
    @Test fun partialPasteStartsAtCurrentBox() {
        val state = OtpState.initial(0).input(0, "12").input(2, "code: 345678")
        assertEquals("123456", state.digits.joinToString(""))
    }
    @Test fun backspaceOnEmptyClearsPreviousDigit() {
        val state = OtpState.initial(0).input(0, "12").delete(2)
        assertEquals(listOf("1", "", "", "", "", ""), state.digits)
        assertEquals(1, state.active)
    }
    @Test fun backspaceOnFilledClearsCurrent() {
        val state = OtpState.initial(0).input(0, "123456").delete(5)
        assertEquals("12345", state.digits.joinToString(""))
        assertEquals(5, state.active)
    }
    @Test fun deletionAtFirstBoxIsBounded() {
        val state = OtpState.initial(0).delete(0)
        assertEquals(0, state.active)
        assertEquals(List(6) { "" }, state.digits)
    }
    @Test fun incompleteSubmitHasNoEffect() {
        val state = OtpState.initial(0).input(0, "123")
        assertEquals(state, state.submit())
    }
    @Test fun correctCodeShowsExactSuccessWithoutResettingTimer() {
        val state = OtpState.initial(0).input(0, "123456").submit()
        assertEquals("You're in.", state.status)
        assertTrue(state.successful)
        assertEquals(30000L, state.deadlineMillis)
    }
    @Test fun wrongCodeShowsExactErrorClearsAndRefocuses() {
        val state = OtpState.initial(0).input(0, "482913").submit()
        assertEquals("That code didn't work. Try again.", state.status)
        assertEquals(List(6) { "" }, state.digits)
        assertEquals(0, state.active)
        assertEquals(1, state.focusRequest)
    }
    @Test fun countdownRoundsUpAndStopsAtZero() {
        val state = OtpState.initial(1000)
        assertEquals(1, state.secondsRemaining(30001))
        assertEquals(0, state.secondsRemaining(31000))
        assertEquals(0, state.secondsRemaining(90000))
    }
    @Test fun resendIsGatedThenRestartsThirtySeconds() {
        val state = OtpState.initial(0)
        assertEquals(state, state.resend(29999))
        val resent = state.resend(30000)
        assertEquals(30, resent.secondsRemaining(30000))
        assertEquals(0, resent.secondsRemaining(60000))
    }
}
