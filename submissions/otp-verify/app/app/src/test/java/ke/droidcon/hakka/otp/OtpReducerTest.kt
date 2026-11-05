package ke.droidcon.hakka.otp

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpReducerTest {

    private fun enter(vararg digits: String): OtpUiState =
        digits.fold(OtpUiState()) { state, digit ->
            OtpReducer.fieldChanged(state, state.focusedIndex, digit)
        }

    @Test
    fun typingADigitStoresItAndMovesFocusForward() {
        val state = OtpReducer.fieldChanged(OtpUiState(), 0, "4")

        assertEquals(listOf("4", "", "", "", "", ""), state.digits)
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun typingInTheLastBoxKeepsFocusInTheLastBox() {
        val state = enter("1", "2", "3", "4", "5", "6")

        assertEquals("123456", state.code)
        assertEquals(5, state.focusedIndex)
        assertTrue(state.isComplete)
    }

    @Test
    fun nonDigitInputIsRejected() {
        assertEquals(OtpUiState(), OtpReducer.fieldChanged(OtpUiState(), 0, "a"))
    }

    @Test
    fun nonDigitPasteWithoutDigitsIsRejected() {
        assertEquals(OtpUiState(), OtpReducer.fieldChanged(OtpUiState(), 0, "no code here"))
    }

    @Test
    fun pasteExtractsDigitsFromArbitraryText() {
        val state = OtpReducer.fieldChanged(
            OtpUiState(),
            0,
            "Your code is 482913",
        )

        assertEquals("482913", state.code)
        assertEquals(5, state.focusedIndex)
        assertTrue(state.isComplete)
    }

    @Test
    fun aFullPasteAlwaysFillsFromTheFirstBox() {
        val state = OtpReducer.fieldChanged(
            OtpUiState(digits = listOf("1", "2", "3", "", "", ""), focusedIndex = 3),
            3,
            OtpConfig.EMPTY_DIGIT_TEXT + "482913",
        )

        assertEquals("482913", state.code)
    }

    @Test
    fun typingOverAFilledBoxReplacesTheDigit() {
        val filled = enter("4")
        val state = OtpReducer.fieldChanged(filled, 0, "49")

        assertEquals(listOf("9", "", "", "", "", ""), state.digits)
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun aFullPasteIntoAFilledBoxUsesThePastedCode() {
        val filled = OtpUiState(digits = listOf("9", "", "", "", "", ""))
        val state = OtpReducer.fieldChanged(filled, 0, "9" + "482913")

        assertEquals("482913", state.code)
        assertEquals(5, state.focusedIndex)
    }

    @Test
    fun backspaceOnAnEmptyBoxClearsThePreviousDigitAndMovesBack() {
        val state = OtpReducer.backspace(enter("4", "8"), 2)

        assertEquals(listOf("4", "", "", "", "", ""), state.digits)
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun backspaceOnAFilledBoxClearsThatDigitAndKeepsFocus() {
        val state = OtpReducer.backspace(enter("4", "8"), 1)

        assertEquals(listOf("4", "", "", "", "", ""), state.digits)
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun backspaceOnTheFirstEmptyBoxDoesNothing() {
        val state = OtpReducer.backspace(OtpUiState(), 0)

        assertEquals(OtpUiState(), state)
    }

    @Test
    fun deletingTheSentinelOnAnEmptyBoxMovesBack() {
        val state = OtpReducer.fieldChanged(enter("4", "8"), 2, "")

        assertEquals(listOf("4", "", "", "", "", ""), state.digits)
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun editingAfterAnErrorClearsTheErrorStatus() {
        val failed = OtpReducer.applyVerificationResult(
            enter("6", "5", "4", "3", "2", "1"),
            OtpVerificationResult.Invalid,
        )

        val editing = OtpReducer.fieldChanged(failed, 0, "1")

        assertEquals(OtpStatus.Idle, editing.status)
        assertEquals(1, editing.focusedIndex)
    }

    @Test
    fun anIncompleteCodeCannotBeSubmitted() {
        val state = enter("1", "2", "3", "4", "5")

        assertFalse(state.canSubmit)
        assertEquals(state, OtpReducer.startVerifying(state))
    }

    @Test
    fun aCompleteCodeStartsVerification() {
        val state = OtpReducer.startVerifying(enter("1", "2", "3", "4", "5", "6"))

        assertTrue(state.isVerifying)
        assertFalse(state.canSubmit)
    }

    @Test
    fun successKeepsTheCodeAndShowsTheSuccessStatus() {
        val state = OtpReducer.applyVerificationResult(
            enter("1", "2", "3", "4", "5", "6"),
            OtpVerificationResult.Success,
        )

        assertEquals("123456", state.code)
        assertEquals(OtpStatus.Success, state.status)
        assertFalse(state.isVerifying)
    }

    @Test
    fun failureClearsEveryBoxAndFocusesTheFirst() {
        val state = OtpReducer.applyVerificationResult(
            enter("6", "5", "4", "3", "2", "1"),
            OtpVerificationResult.Invalid,
        )

        assertEquals(List(OtpConfig.LENGTH) { "" }, state.digits)
        assertEquals(0, state.focusedIndex)
        assertEquals(OtpStatus.Error, state.status)
        assertFalse(state.isVerifying)
    }

    @Test
    fun countdownTicksDownAndStopsAtZero() {
        var state = OtpUiState(secondsUntilResend = 2)

        state = OtpReducer.tick(state)
        assertEquals(1, state.secondsUntilResend)

        state = OtpReducer.tick(state)
        assertEquals(0, state.secondsUntilResend)

        state = OtpReducer.tick(state)
        assertEquals(0, state.secondsUntilResend)
        assertTrue(state.canResend)
    }

    @Test
    fun resendOnlyWorksAfterTheCountdownAndRestartsIt() {
        val waiting = OtpUiState(secondsUntilResend = 3)

        assertEquals(waiting, OtpReducer.resend(waiting))

        val finished = OtpUiState(secondsUntilResend = 0)
        val restarted = OtpReducer.resend(finished)

        assertEquals(OtpConfig.RESEND_SECONDS, restarted.secondsUntilResend)
        assertFalse(restarted.canResend)
    }

    @Test
    fun focusIsClampedToTheDigits() {
        assertEquals(0, OtpReducer.focus(OtpUiState(), -4).focusedIndex)
        assertEquals(5, OtpReducer.focus(OtpUiState(), 99).focusedIndex)
    }

    @Test
    fun localVerifierAcceptsOnlyTheConfiguredCode() = runBlocking {
        val verifier = LocalOtpVerifier(validCode = "123456")

        assertEquals(OtpVerificationResult.Success, verifier.verify("123456"))
        assertEquals(OtpVerificationResult.Invalid, verifier.verify("654321"))
        assertEquals(OtpVerificationResult.Invalid, verifier.verify("12345"))
    }
}
