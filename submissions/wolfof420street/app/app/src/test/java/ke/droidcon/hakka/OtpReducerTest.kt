package ke.droidcon.hakka

import ke.droidcon.hakka.domain.model.OtpReducer
import ke.droidcon.hakka.domain.model.OtpState
import ke.droidcon.hakka.domain.model.OtpStatus
import ke.droidcon.hakka.domain.model.RESEND_SECONDS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpReducerTest {

    private fun typed(code: String): OtpState =
        code.foldIndexed(OtpState()) { index, state, char ->
            OtpReducer.input(state, index, char.toString())
        }

    @Test
    fun typingADigitMovesFocusToTheNextBox() {
        val state = OtpReducer.input(OtpState(), 0, "4")

        assertEquals("4", state.digits[0])
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun severalDigitsArrivingAtOnceFillForwardFromTheBox() {
        val state = OtpReducer.input(typed("215"), 3, "99")

        assertEquals("21599", state.code)
        assertEquals(5, state.focusedIndex)
    }

    @Test
    fun nonDigitsAreIgnored() {
        assertEquals(OtpState(), OtpReducer.input(OtpState(), 0, "a"))
        assertEquals(OtpState(), OtpReducer.input(OtpState(), 0, "@#$"))
    }

    @Test
    fun backspaceOnAFilledBoxClearsTheCurrentDigit() {
        val state = OtpReducer.backspace(typed("215"), 2)

        assertEquals(listOf("2", "1", "", "", "", ""), state.digits)
        assertEquals(2, state.focusedIndex)
    }

    @Test
    fun backspaceOnAnEmptyBoxClearsThePreviousDigit() {
        val state = OtpReducer.backspace(typed("21"), 2)

        assertEquals(listOf("2", "", "", "", "", ""), state.digits)
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun backspaceOnFirstBoxWhenEmptyDoesNothing() {
        val state = OtpReducer.backspace(OtpState(), 0)
        assertEquals(OtpState(), state)
    }

    @Test
    fun pastingTextFillsAllSixBoxes() {
        val state = OtpReducer.input(OtpState(), 0, "Your code is 482913")

        assertEquals("482913", state.code)
        assertTrue(state.isComplete)
    }

    @Test
    fun submitIsOnlyPossibleWhenAllDigitsAreFilled() {
        assertFalse(typed("12345").isComplete)
        assertEquals(OtpStatus.Idle, OtpReducer.submit(typed("12345"), true).status)
    }

    @Test
    fun theValidCodeSucceeds() {
        val state = OtpReducer.submit(typed("123456"), true)
        assertEquals(OtpStatus.Success, state.status)
        assertEquals("123456", state.code)
    }

    @Test
    fun aWrongCodeShowsAnErrorClearsTheBoxesAndFocusesTheFirst() {
        val state = OtpReducer.submit(typed("654321"), false)

        assertEquals(OtpStatus.Error, state.status)
        assertEquals("", state.code)
        assertEquals(0, state.focusedIndex)
    }

    @Test
    fun resendIsDisabledUntilTheCountdownEndsThenRestartsIt() {
        val waiting = OtpState(resendSecondsLeft = 3)
        assertEquals(waiting, OtpReducer.resend(waiting))

        val finished = (1..3).fold(waiting) { state, _ -> OtpReducer.tick(state) }
        assertTrue(finished.canResend)
        assertEquals(RESEND_SECONDS, OtpReducer.resend(finished).resendSecondsLeft)
    }
}
