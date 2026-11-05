package ke.droidcon.hakka.otp

import androidx.compose.ui.text.input.KeyboardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** One test per acceptance criterion in issue #1, driven through the pure [OtpStateHolder]. */
class OtpAcceptanceTest {

    private val holder = OtpStateHolder()
    private val state get() = holder.state

    private fun type(code: String) = code.forEach { holder.onInput(state.focusedIndex, it.toString()) }

    @Test
    fun ac1_sixDigitBoxes_digitsOnly_numericKeyboard_firstBoxFocused() {
        assertEquals(6, state.digits.size)
        assertEquals(0, state.focusedIndex)
        assertEquals(KeyboardType.Number, OtpKeyboardOptions.keyboardType)

        holder.onInput(0, "a")
        holder.onInput(0, "-")
        holder.onInput(0, " ")
        assertEquals("", state.code)
        assertEquals(0, state.focusedIndex)

        holder.onInput(0, "7")
        assertEquals("7", state.code)
    }

    @Test
    fun ac2_boxesAreFilledFocusedOrEmpty_andEmptyIsNeverAZero() {
        type("21")
        assertEquals(DigitBoxState.Filled, state.boxState(0))
        assertEquals(DigitBoxState.Filled, state.boxState(1))
        assertEquals(DigitBoxState.Focused, state.boxState(2))
        (3..5).forEach { assertEquals(DigitBoxState.Empty, state.boxState(it)) }
        (2..5).forEach { assertEquals(null, state.digits[it]) }
        assertEquals("21", state.code)
    }

    @Test
    fun ac3_typingADigitMovesFocusToTheNextBox() {
        holder.onInput(0, "4")
        assertEquals(1, state.focusedIndex)
        holder.onInput(1, "8")
        assertEquals(2, state.focusedIndex)
        assertEquals(listOf('4', '8', null, null, null, null), state.digits)
    }

    @Test
    fun ac4_backspaceOnEmptyBoxMovesBackAndClearsPreviousDigit() {
        type("482")
        assertEquals(3, state.focusedIndex)

        holder.onBackspace(3)
        assertEquals(2, state.focusedIndex)
        assertEquals("48", state.code)

        holder.onBackspace(2)
        assertEquals(1, state.focusedIndex)
        assertEquals("4", state.code)
    }

    @Test
    fun ac5_pastingTextExtractsTheSixDigits() {
        holder.onPaste("Your code is 482913")
        assertEquals("482913", state.code)
        assertTrue(state.isComplete)

        // Pasting through a box's text field (e.g. a partly filled box) also works.
        val viaField = OtpStateHolder()
        viaField.onInput(0, "1")
        viaField.onInput(1, "Your code is 482913")
        assertEquals("482913", viaField.state.code)
    }

    @Test
    fun ac6_submitIsDisabledUntilAllSixDigitsAreFilled() {
        assertFalse(state.canSubmit)
        type("12345")
        assertFalse(state.canSubmit)
        holder.onSubmit()
        assertEquals(OtpStatus.Idle, state.status)

        type("6")
        assertTrue(state.canSubmit)
    }

    @Test
    fun ac7_correctCodeShowsSuccess() {
        type("123456")
        holder.onSubmit()
        assertEquals(OtpStatus.Success, state.status)
        assertEquals("123456", state.code)
    }

    @Test
    fun ac8_wrongCodeShowsErrorClearsBoxesAndRefocusesFirstBox() {
        type("654321")
        holder.onSubmit()
        assertEquals(OtpStatus.Error, state.status)
        assertEquals(List<Char?>(6) { null }, state.digits)
        assertEquals(0, state.focusedIndex)
        assertFalse(state.canSubmit)
    }

    @Test
    fun ac9_resendIsDisabledFor30SecondCountdownThenRestarts() {
        assertEquals(30, state.resendSecondsLeft)
        assertEquals("0:30", formatCountdown(state.resendSecondsLeft))
        assertFalse(state.canResend)

        holder.onResend() // ignored while counting down
        repeat(25) { holder.onTick() }
        assertEquals("0:05", formatCountdown(state.resendSecondsLeft))
        assertFalse(state.canResend)

        repeat(5) { holder.onTick() }
        assertTrue(state.canResend)
        holder.onTick()
        assertEquals(0, state.resendSecondsLeft)

        holder.onResend()
        assertEquals(30, state.resendSecondsLeft)
        assertFalse(state.canResend)
    }

    @Test
    fun ac10_boxesAreLabelledForScreenReaders_andColoursExistForLightAndDark() {
        assertEquals(
            listOf("Digit 1 of 6", "Digit 2 of 6", "Digit 3 of 6", "Digit 4 of 6", "Digit 5 of 6", "Digit 6 of 6"),
            (0 until OTP_LENGTH).map(::digitContentDescription),
        )

        val json = File("src/main/assets/colours.json").readText()
        val light = ColourTokens.parse(json, ColourTokens.LIGHT)
        val dark = ColourTokens.parse(json, ColourTokens.DARK)
        assertEquals(light.keys, dark.keys)
        assertEquals(0xFF12361BL, light.getValue("digit_filled_bg"))
        assertEquals(0x99000000L, light.getValue("scrim"))
        assertNotEquals(light.getValue("sheet"), dark.getValue("sheet"))
        assertNotEquals(light.getValue("text_primary"), dark.getValue("text_primary"))
    }
}
