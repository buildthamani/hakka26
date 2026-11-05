package ke.droidcon.hakka.ui.otp

import ke.droidcon.hakka.domain.FixedOtpVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpReducerTest {
    private val reducer = OtpReducer(FixedOtpVerifier("123456"))

    private fun OtpState.reduce(vararg actions: OtpAction) =
        actions.fold(this) { state, action -> reducer.reduce(state, action) }

    @Test
    fun typingDigitAdvancesFocus() {
        val state = OtpState().reduce(OtpAction.Input("2"))
        assertEquals("2", state.code)
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun nonDigitsAreIgnored() {
        assertEquals("12", OtpState().reduce(OtpAction.Input("1a2-")).code)
    }

    @Test
    fun backspaceRemovesPreviousDigit() {
        val state = OtpState(code = "215").reduce(OtpAction.Input("21"))
        assertEquals("21", state.code)
        assertEquals(2, state.focusedIndex)
    }

    @Test
    fun pasteExtractsDigitsFromMessage() {
        assertEquals("482913", OtpState().reduce(OtpAction.Input("Your code is 482913")).code)
    }

    @Test
    fun pasteOverPartialCodeReplacesIt() {
        assertEquals("482913", OtpState(code = "21").reduce(OtpAction.Input("21Your code is 482913")).code)
    }

    @Test
    fun extraKeystrokeOnFullCodeIsIgnored() {
        assertEquals("123456", OtpState(code = "123456").reduce(OtpAction.Input("1234567")).code)
    }

    @Test
    fun submitDisabledUntilSixDigits() {
        val partial = OtpState(code = "12345")
        assertFalse(partial.canSubmit)
        assertEquals(partial, partial.reduce(OtpAction.Submit))
        assertTrue(OtpState(code = "123456").canSubmit)
    }

    @Test
    fun correctCodeSucceeds() {
        val state = OtpState().reduce(OtpAction.Input("123456"), OtpAction.Submit)
        assertEquals(OtpStatus.Success, state.status)
    }

    @Test
    fun wrongCodeShowsErrorClearsBoxesAndRefocusesFirst() {
        val state = OtpState().reduce(OtpAction.Input("654321"), OtpAction.Submit)
        assertEquals(OtpStatus.Error, state.status)
        assertEquals("", state.code)
        assertEquals(0, state.focusedIndex)
    }

    @Test
    fun typingAfterErrorClearsMessage() {
        val state = OtpState(status = OtpStatus.Error).reduce(OtpAction.Input("1"))
        assertEquals(OtpStatus.Idle, state.status)
    }

    @Test
    fun resendDisabledDuringCountdown() {
        val state = OtpState().reduce(OtpAction.Tick, OtpAction.Resend)
        assertEquals(RESEND_SECONDS - 1, state.resendSecondsLeft)
        assertFalse(state.canResend)
    }

    @Test
    fun countdownStopsAtZeroThenResendRestartsIt() {
        val ticks = Array<OtpAction>(RESEND_SECONDS + 5) { OtpAction.Tick }
        val expired = OtpState().reduce(*ticks)
        assertEquals(0, expired.resendSecondsLeft)
        assertTrue(expired.canResend)
        assertEquals(RESEND_SECONDS, expired.reduce(OtpAction.Resend).resendSecondsLeft)
    }

    @Test
    fun countdownFormatsAsMinutesSeconds() {
        assertEquals("0:30", formatCountdown(30))
        assertEquals("0:05", formatCountdown(5))
        assertEquals("0:00", formatCountdown(0))
    }
}
