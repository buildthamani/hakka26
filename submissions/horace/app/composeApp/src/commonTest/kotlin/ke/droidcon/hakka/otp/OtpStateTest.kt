package ke.droidcon.hakka.otp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OtpStateTest {
    private fun newState() = OtpState(verify = DemoOtpVerifier::verify)

    private fun OtpState.typeAll(code: String) = code.forEachIndexed { i, c -> enter(i, c.toString()) }

    @Test
    fun startsEmptyWithFocusOnFirstBox() {
        val state = newState()
        assertEquals(6, state.digits.size)
        assertTrue(state.digits.all { it.isEmpty() })
        assertEquals(0, state.focusedIndex)
        assertFalse(state.isComplete)
    }

    @Test
    fun typingADigitAdvancesFocus() {
        val state = newState()
        state.enter(0, "4")
        assertEquals("4", state.digits[0])
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun nonDigitsAreRejected() {
        val state = newState()
        state.enter(0, "a")
        state.enter(0, " ")
        state.enter(0, "-")
        assertTrue(state.digits.all { it.isEmpty() })
        assertEquals(0, state.focusedIndex)
    }

    @Test
    fun backspaceOnFilledBoxClearsIt() {
        val state = newState()
        state.enter(0, "1")
        state.enter(1, "2")
        state.backspace(1)
        assertEquals("", state.digits[1])
        assertEquals("1", state.digits[0])
    }

    @Test
    fun backspaceOnEmptyBoxClearsPreviousAndMovesBack() {
        val state = newState()
        state.enter(0, "1")
        state.enter(1, "2")
        assertEquals(2, state.focusedIndex)
        state.backspace(2)
        assertEquals("", state.digits[1])
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun backspaceOnFirstEmptyBoxDoesNothing() {
        val state = newState()
        state.backspace(0)
        assertEquals(0, state.focusedIndex)
    }

    @Test
    fun pastingTextExtractsTheDigits() {
        val state = newState()
        state.enter(0, "Your code is 482913")
        assertEquals("482913", state.code)
        assertTrue(state.isComplete)
    }

    @Test
    fun pastingAFullCodeIntoAnyBoxFillsFromTheStart() {
        val state = newState()
        state.enter(3, "Your code is 482913")
        assertEquals("482913", state.code)
    }

    @Test
    fun pastingAPartialCodeFillsFromTheCurrentBox() {
        val state = newState()
        state.enter(0, "9")
        state.enter(1, "87")
        assertEquals(listOf("9", "8", "7", "", "", ""), state.digits.toList())
        assertEquals(3, state.focusedIndex)
    }

    @Test
    fun submitDoesNothingUntilAllSixDigitsAreFilled() {
        val state = newState()
        state.typeAll("12345")
        assertFalse(state.isComplete)
        state.submit()
        assertEquals(OtpStatus.Idle, state.status)
        assertEquals("12345", state.code)
    }

    @Test
    fun correctCodeSucceeds() {
        val state = newState()
        state.typeAll("123456")
        state.submit()
        assertEquals(OtpStatus.Success, state.status)
    }

    @Test
    fun wrongCodeShowsErrorClearsBoxesAndRefocusesFirst() {
        val state = newState()
        state.typeAll("654321")
        val requestsBefore = state.focusRequest
        state.submit()
        assertEquals(OtpStatus.Error, state.status)
        assertTrue(state.digits.all { it.isEmpty() })
        assertEquals(0, state.focusedIndex)
        assertTrue(state.focusRequest > requestsBefore)
    }

    @Test
    fun typingAfterAnErrorClearsTheError() {
        val state = newState()
        state.typeAll("000000")
        state.submit()
        state.enter(0, "1")
        assertEquals(OtpStatus.Idle, state.status)
    }

    @Test
    fun verifierReceivesTheEnteredCode() {
        var received: String? = null
        val state = OtpState(verify = { received = it; false })
        state.typeAll("482913")
        state.submit()
        assertEquals("482913", received)
    }

    @Test
    fun resendIsDisabledForThirtySecondsThenEnabled() {
        val state = newState()
        assertEquals(30, state.resendSecondsRemaining)
        assertFalse(state.canResend)
        state.resend()
        assertEquals(0, state.resendCount, "resend ignored during countdown")
        repeat(29) { state.tick() }
        assertFalse(state.canResend)
        state.tick()
        assertTrue(state.canResend)
        state.tick()
        assertEquals(0, state.resendSecondsRemaining)
    }

    @Test
    fun resendRestartsTheCountdown() {
        val state = newState()
        repeat(30) { state.tick() }
        state.resend()
        assertEquals(30, state.resendSecondsRemaining)
        assertEquals(1, state.resendCount)
        assertFalse(state.canResend)
    }

    @Test
    fun countdownCopyUsesMinutesAndPaddedSeconds() {
        assertEquals("0:30", formatCountdown(30))
        assertEquals("0:05", formatCountdown(5))
        assertEquals("0:00", formatCountdown(0))
        assertEquals("Resend in 0:09", OtpCopy.resendIn(9))
    }

    @Test
    fun digitLabelsAreOneBased() {
        assertEquals("Digit 1 of 6", OtpCopy.digitLabel(0, 6))
        assertEquals("Digit 6 of 6", OtpCopy.digitLabel(5, 6))
    }

    @Test
    fun insertedTextIgnoresTheExistingDigit() {
        assertEquals("7", insertedText("", "7"))
        assertEquals("7", insertedText("3", "37"))
        assertEquals("7", insertedText("3", "73"))
        assertEquals("Your code is 482913", insertedText("3", "3Your code is 482913"))
    }
}
