package ke.droidcon.hakka.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpReducerTest {

    private fun OtpState.dispatch(vararg actions: OtpAction) = actions.fold(this, ::reduce)

    private fun OtpState.type(code: String) =
        code.fold(this) { state, char -> reduce(state, OtpAction.Input(state.focusedIndex, char.toString())) }

    @Test
    fun startsEmptyOnTheFirstBoxWithResendCoolingDown() {
        val state = OtpState()

        assertEquals(List(OTP_LENGTH) { null }, state.digits)
        assertEquals(0, state.focusedIndex)
        assertFalse(state.isComplete)
        assertEquals(RESEND_COOLDOWN_SECONDS, state.resendSecondsLeft)
        assertFalse(state.canResend)
    }

    @Test
    fun typingADigitFillsTheBoxAndMovesFocusToTheNext() {
        val state = OtpState().dispatch(OtpAction.Input(0, "7"))

        assertEquals('7', state.digits[0])
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun nonDigitInputIsRejected() {
        val state = OtpState().dispatch(OtpAction.Input(0, "a"), OtpAction.Input(0, " "), OtpAction.Input(0, "-"))

        assertEquals(OtpState(), state)
    }

    @Test
    fun typingIntoTheLastBoxKeepsFocusThere() {
        val state = OtpState().type("123456")

        assertEquals("123456", state.code)
        assertEquals(OTP_LENGTH - 1, state.focusedIndex)
        assertTrue(state.isComplete)
    }

    @Test
    fun backspaceOnAnEmptyBoxClearsThePreviousDigitAndMovesBack() {
        val state = OtpState().type("21").dispatch(OtpAction.Backspace(2))

        assertEquals(listOf('2', null, null, null, null, null), state.digits)
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun backspaceOnAFilledBoxClearsItAndKeepsFocus() {
        val state = OtpState().type("123456").dispatch(OtpAction.Backspace(5))

        assertEquals("12345", state.code)
        assertEquals(5, state.focusedIndex)
    }

    @Test
    fun backspaceOnAnEmptyFirstBoxDoesNothing() {
        assertEquals(OtpState(), OtpState().dispatch(OtpAction.Backspace(0)))
    }

    @Test
    fun pastingAMessageExtractsTheCodeIntoAllSixBoxes() {
        val state = OtpState().type("9").dispatch(OtpAction.Input(3, "Your code is 482913"))

        assertEquals(listOf('4', '8', '2', '9', '1', '3'), state.digits)
        assertEquals(OTP_LENGTH - 1, state.focusedIndex)
    }

    @Test
    fun pastingAPartialCodeFillsFromTheReceivingBox() {
        val state = OtpState().dispatch(OtpAction.Input(2, "12"))

        assertEquals(listOf(null, null, '1', '2', null, null), state.digits)
        assertEquals(4, state.focusedIndex)
    }

    @Test
    fun submitIsIgnoredUntilAllSixDigitsAreFilled() {
        val partial = OtpState().type("12345")

        assertFalse(partial.isComplete)
        assertEquals(partial, partial.dispatch(OtpAction.Submit))
    }

    @Test
    fun submittingTheAcceptedCodeSucceeds() {
        val state = OtpState().type("123456").dispatch(OtpAction.Submit)

        assertEquals(OtpStatus.Success, state.status)
    }

    @Test
    fun submittingAnyOtherCodeFailsClearsTheBoxesAndRefocusesTheFirst() {
        val state = OtpState().type("654321").dispatch(OtpAction.Submit)

        assertEquals(OtpStatus.Error, state.status)
        assertEquals(List(OTP_LENGTH) { null }, state.digits)
        assertEquals(0, state.focusedIndex)
    }

    @Test
    fun editingAfterAnErrorClearsTheMessage() {
        val state = OtpState().type("000000").dispatch(OtpAction.Submit).type("1")

        assertEquals(OtpStatus.Idle, state.status)
    }

    @Test
    fun countdownTicksDownToZeroThenAllowsResend() {
        val ticks = generateSequence(OtpState()) { reduce(it, OtpAction.Tick) }
        val almostDone = ticks.elementAt(RESEND_COOLDOWN_SECONDS - 1)
        val done = ticks.elementAt(RESEND_COOLDOWN_SECONDS + 5)

        assertEquals(1, almostDone.resendSecondsLeft)
        assertFalse(almostDone.canResend)
        assertEquals(0, done.resendSecondsLeft)
        assertTrue(done.canResend)
    }

    @Test
    fun resendIsIgnoredDuringTheCountdown() {
        val counting = OtpState().dispatch(OtpAction.Tick)

        assertEquals(counting, counting.dispatch(OtpAction.Resend))
    }

    @Test
    fun resendRestartsTheCountdown() {
        val state = OtpState(resendSecondsLeft = 0).dispatch(OtpAction.Resend)

        assertEquals(RESEND_COOLDOWN_SECONDS, state.resendSecondsLeft)
        assertFalse(state.canResend)
    }
}
