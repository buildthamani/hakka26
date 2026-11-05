package ke.droidcon.hakka

import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@Suppress("DEPRECATION")
class OtpScreenTest {
    @get:Rule val rule = createComposeRule()
    private var now = 0L
    private lateinit var clipboard: ClipboardManager
    private fun launch(dark: Boolean = false) {
        rule.setContent {
            clipboard = LocalClipboardManager.current
            HakkaTheme(darkTheme = dark) { OtpScreen(nowMillis = { now }) }
        }
    }
    private fun digit(index: Int) = rule.onNodeWithTag("otp_digit_${index + 1}")
    private fun assertDigit(index: Int, value: String) {
        digit(index).assert(SemanticsMatcher.expectValue(SemanticsProperties.InputText,
            AnnotatedString(value.ifEmpty { EMPTY_SLOT })))
    }
    private fun paste(text: String, index: Int = 0) {
        digit(index).performClick()
        rule.runOnIdle { clipboard.setText(AnnotatedString(text)) }
        digit(index).performSemanticsAction(SemanticsActions.PasteText) { it() }
    }
    private fun advance(seconds: Int) {
        rule.runOnIdle { now += seconds * 1000L }
        rule.mainClock.advanceTimeBy(200)
        rule.waitForIdle()
    }
    @Test fun initialScreenHasExactCopyFocusPlaceholdersAndDisabledActions() {
        launch()
        rule.onNodeWithText("Enter your code").assertIsDisplayed()
        rule.onNodeWithText("A 6-digit code has been sent to your phone.").assertIsDisplayed()
        rule.onNodeWithText("Didn't receive the code?").assertIsDisplayed()
        rule.onNodeWithText("Resend in 0:30").assertIsDisplayed()
        repeat(6) { assertDigit(it, "") }
        digit(0).assertIsFocused().assertIsSelected()
        rule.onNodeWithTag("otp_submit").assertIsNotEnabled()
        rule.onNodeWithTag("otp_resend").assertIsNotEnabled()
    }
    @Test fun visibleDigitsAdvanceFocusAndEnableSubmitOnlyWhenComplete() {
        launch()
        "123456".forEachIndexed { index, char ->
            rule.onNodeWithTag("otp_submit").assertIsNotEnabled()
            digit(index).performTextInput(char.toString())
            assertDigit(index, char.toString())
            digit(index).assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,
                AnnotatedString(char.toString())))
            digit((index + 1).coerceAtMost(5)).assertIsFocused()
        }
        rule.onNodeWithTag("otp_submit").assertIsEnabled()
    }
    @Test fun realClipboardPasteExtractsNumbersFromMessageAndDropsOverflow() {
        launch()
        paste("Your code is 482913789")
        "482913".forEachIndexed { index, char -> assertDigit(index, char.toString()) }
        digit(5).performTextInput("789")
        assertDigit(5, "3")
    }
    @Test fun ignoresNonNumericTextAndAcceptsLeadingZero() {
        launch()
        digit(0).performTextInput("abc")
        assertDigit(0, "")
        digit(0).performTextInput("0")
        assertDigit(0, "0")
        digit(1).assertIsFocused()
    }
    @Test fun hardwareBackspaceOnEmptyClearsPreviousAndMovesBack() {
        launch()
        paste("12")
        digit(2).performKeyInput { pressKey(Key.Backspace) }
        assertDigit(1, "")
        assertDigit(0, "1")
        digit(1).assertIsFocused()
    }
    @Test fun softwareBackspaceOnEmptyClearsPreviousAndMovesBack() {
        launch()
        paste("12")
        digit(2).performTextClearance()
        assertDigit(1, "")
        assertDigit(0, "1")
        digit(1).assertIsFocused()
    }
    @Test fun deleteFilledBoxClearsItAndFirstBoxNeverGoesOutOfBounds() {
        launch()
        paste("123456")
        digit(5).performKeyInput { pressKey(Key.Delete) }
        assertDigit(5, "")
        assertDigit(4, "5")
        digit(5).assertIsFocused()
        digit(0).performClick().performTextClearance()
        digit(0).performTextClearance()
        assertDigit(0, "")
        digit(0).assertIsFocused()
    }
    @Test fun correctCodeShowsExactSuccess() {
        launch()
        paste("123456")
        rule.onNodeWithTag("otp_submit").performClick()
        rule.onNodeWithTag("otp_status").assertTextEquals("You're in.")
    }
    @Test fun incorrectCodeShowsErrorClearsAndRefocuses() {
        launch()
        paste("482913")
        rule.onNodeWithTag("otp_submit").performClick()
        rule.onNodeWithTag("otp_status").assertTextEquals("That code didn't work. Try again.")
        repeat(6) { assertDigit(it, "") }
        digit(0).assertIsFocused()
        rule.onNodeWithTag("otp_submit").assertIsNotEnabled()
    }
    @Test fun timerStartsImmediatelyExpiresAndResendRestartsIt() {
        launch()
        advance(29)
        rule.onNodeWithText("Resend in 0:01").assertIsDisplayed()
        rule.onNodeWithTag("otp_resend").assertIsNotEnabled()
        advance(1)
        rule.onNodeWithTag("otp_resend").assertIsEnabled().performClick()
        rule.onNodeWithText("Resend in 0:30").assertIsDisplayed()
        rule.onNodeWithTag("otp_resend").assertIsNotEnabled()
    }
    @Test fun restoresDigitsAndCountdownAcrossRecreation() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            clipboard = LocalClipboardManager.current
            HakkaTheme { OtpScreen(nowMillis = { now }) }
        }
        paste("12")
        advance(10)
        restoration.emulateSavedInstanceStateRestore()
        assertDigit(0, "1")
        assertDigit(1, "2")
        rule.onNodeWithText("Resend in 0:20").assertIsDisplayed()
    }
    @Test fun lightBoxStatesUseProvidedPalette() {
        launch()
        paste("12")
        fun color(index: Int) = digit(index).captureToImage().toPixelMap().let {
            it[it.width / 2, it.height / 4]
        }
        assertEquals(OtpColors.Light.filled, color(0))
        assertEquals(OtpColors.Light.focused, color(2))
        assertEquals(OtpColors.Light.empty, color(3))
    }
    @Test fun darkThemeHasAccessibleLabelsVisibleDigitsAndImeSuccess() {
        launch(dark = true)
        repeat(6) { digit(it).assertContentDescriptionEquals("Digit ${it + 1} of 6") }
        paste("123456")
        val pixels = digit(0).captureToImage().toPixelMap()
        assertEquals(OtpColors.Dark.filled, pixels[pixels.width / 2, pixels.height / 4])
        digit(5).performImeAction()
        rule.onNodeWithTag("otp_status").assertTextEquals("You're in.")
    }
}
