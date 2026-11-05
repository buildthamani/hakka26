package ke.droidcon.hakka.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpStateTest {

    private val verifier = DemoOtpVerifier()

    private fun typed(code: String): OtpState = code.fold(OtpState()) { state, c -> state.typeDigit(c) }

    @Test
    fun initialStateFocusesFirstBoxAndStartsCountdown() {
        val state = OtpState()
        assertEquals(0, state.focusedIndex)
        assertEquals(DigitState.Focused, state.digitState(0))
        (1 until OTP_LENGTH).forEach { assertEquals(DigitState.Empty, state.digitState(it)) }
        assertEquals(RESEND_COOLDOWN_SECONDS, state.resendSecondsLeft)
        assertFalse(state.canResend)
        assertFalse(state.canSubmit)
    }

    @Test
    fun typingDigitFillsBoxAndAdvancesFocus() {
        val state = OtpState().typeDigit('4')
        assertEquals('4', state.digits[0])
        assertEquals(1, state.focusedIndex)
        assertEquals(DigitState.Filled, state.digitState(0))
        assertEquals(DigitState.Focused, state.digitState(1))
    }

    @Test
    fun nonDigitsAreRejected() {
        val state = OtpState()
        assertSame(state, state.typeDigit('a'))
        assertSame(state, state.typeDigit(' '))
        assertSame(state, state.paste("no digits here"))
    }

    @Test
    fun typingIntoCompleteCodeIsIgnored() {
        val full = typed("123456")
        assertSame(full, full.typeDigit('7'))
        assertEquals(OTP_LENGTH, full.focusedIndex)
    }

    @Test
    fun backspaceOnEmptyBoxMovesBackAndClearsPreviousDigit() {
        val state = typed("12").backspace()
        assertEquals(listOf('1', null, null, null, null, null), state.digits)
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun backspaceOnFilledFocusedBoxClearsItInPlace() {
        val state = typed("123").focus(1).backspace()
        assertEquals(listOf('1', null, '3', null, null, null), state.digits)
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun backspaceAfterCompleteCodeClearsLastDigit() {
        val state = typed("123456").backspace()
        assertEquals("12345", state.code)
        assertEquals(5, state.focusedIndex)
    }

    @Test
    fun backspaceOnFirstEmptyBoxDoesNothing() {
        val state = OtpState()
        assertSame(state, state.backspace())
    }

    @Test
    fun pasteExtractsDigitsFromText() {
        val state = OtpState().paste("Your code is 482913")
        assertEquals("482913", state.code)
        assertTrue(state.isComplete)
        assertEquals(OTP_LENGTH, state.focusedIndex)
    }

    @Test
    fun pasteOfFullCodeReplacesExistingDigits() {
        val state = typed("99").paste("Your code is 482913")
        assertEquals("482913", state.code)
    }

    @Test
    fun pasteOfPartialCodeInsertsFromFocusedBox() {
        val state = typed("1").paste("23")
        assertEquals("123", state.code)
        assertEquals(3, state.focusedIndex)
    }

    @Test
    fun pasteKeepsOnlyFirstSixDigits() {
        assertEquals("123456", OtpState().paste("1234567890").code)
    }

    @Test
    fun submitIsDisabledUntilAllSixDigitsAreFilled() {
        var state = OtpState()
        "12345".forEach { c ->
            state = state.typeDigit(c)
            assertFalse(state.canSubmit)
        }
        assertSame(state, state.submit(verifier))
        assertTrue(state.typeDigit('6').canSubmit)
    }

    @Test
    fun correctCodeShowsSuccess() {
        val state = typed("123456").submit(verifier)
        assertEquals(OtpStatus.Success, state.status)
        assertEquals("123456", state.code)
    }

    @Test
    fun wrongCodeShowsErrorClearsBoxesAndRefocusesFirstBox() {
        val state = typed("654321").tick().submit(verifier)
        assertEquals(OtpStatus.Error, state.status)
        state.digits.forEach { assertNull(it) }
        assertEquals(0, state.focusedIndex)
        assertEquals(RESEND_COOLDOWN_SECONDS - 1, state.resendSecondsLeft)
    }

    @Test
    fun typingAfterErrorClearsTheStatus() {
        val state = typed("000000").submit(verifier).typeDigit('1')
        assertEquals(OtpStatus.None, state.status)
    }

    @Test
    fun verifierIsUsedRatherThanAHardCodedCode() {
        val acceptsAnything = OtpVerifier { true }
        assertEquals(OtpStatus.Success, typed("999999").submit(acceptsAnything).status)
    }

    @Test
    fun resendIsDisabledDuringCountdownThenEnabledAndRestarts() {
        var state = OtpState()
        assertSame(state, state.resend())
        repeat(RESEND_COOLDOWN_SECONDS - 1) { state = state.tick() }
        assertEquals(1, state.resendSecondsLeft)
        assertFalse(state.canResend)

        state = state.tick()
        assertTrue(state.canResend)
        assertSame(state, state.tick())

        state = state.resend()
        assertEquals(RESEND_COOLDOWN_SECONDS, state.resendSecondsLeft)
        assertFalse(state.canResend)
    }

    @Test
    fun countdownIsFormattedAsMinutesAndSeconds() {
        assertEquals("0:30", formatCountdown(30))
        assertEquals("0:05", formatCountdown(5))
        assertEquals("0:00", formatCountdown(0))
    }

    @Test
    fun tappedBoxBecomesFocusedEvenWhenFilled() {
        val state = typed("215").focus(2)
        assertEquals(DigitState.Filled, state.digitState(0))
        assertEquals(DigitState.Focused, state.digitState(2))
        assertEquals(DigitState.Empty, state.digitState(3))
    }
}
