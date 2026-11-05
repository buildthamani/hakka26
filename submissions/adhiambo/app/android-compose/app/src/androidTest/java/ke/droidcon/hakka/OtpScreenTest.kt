package ke.droidcon.hakka

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.text.AnnotatedString
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

    private fun digit(index: Int) = rule.onNodeWithTag("digit-$index")
    private fun assertDigit(index: Int, value: String) {
        digit(index).assert(SemanticsMatcher.expectValue(
            SemanticsProperties.InputText, AnnotatedString(value.ifEmpty { "\u200B" })))
    }
    private fun paste(text: String, index: Int = 0) {
        digit(index).performClick()
        rule.runOnIdle { clipboard.setText(AnnotatedString(text)) }
        digit(index).performSemanticsAction(SemanticsActions.PasteText) { it() }
    }
    private fun advance(seconds: Int) {
        rule.runOnIdle { now += seconds * 1_000L }
        rule.mainClock.advanceTimeBy(200)
        rule.waitForIdle()
    }

    @Test fun startsEmptyWithFirstBoxSelectedAndActionsDisabled() {
        launch()
        repeat(6) { assertDigit(it, "") }
        digit(0).assertIsFocused().assertIsSelected()
        rule.onNodeWithTag("submit").assertIsNotEnabled()
        rule.onNodeWithTag("resend").assertIsNotEnabled()
        advance(120)
        rule.onNodeWithTag("countdown").assertTextEquals("Submit your code to start the retry timer")
    }

    @Test fun typingMasksEachDigitAndAdvancesFocus() {
        launch()
        "123456".forEachIndexed { index, char ->
            digit(index).performTextInput(char.toString())
            assertDigit(index, char.toString())
            digit(index).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
            digit(index).assert(SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText, AnnotatedString("•")))
            digit((index + 1).coerceAtMost(5)).assertIsFocused().assertIsSelected()
        }
        rule.onNodeWithTag("submit").assertIsEnabled()
    }

    @Test fun pasteAcceptsSixDigitsAndDropsOverflow() {
        launch()
        paste("123456789")
        "123456".forEachIndexed { index, char -> assertDigit(index, char.toString()) }
        digit(5).assertIsFocused()
        rule.onNodeWithTag("submit").assertIsEnabled()
    }

    @Test fun typingBeyondSixthDigitIsDropped() {
        launch()
        paste("123456")
        digit(5).performTextInput("789")
        "123456".forEachIndexed { index, char -> assertDigit(index, char.toString()) }
    }

    @Test fun softwareDeleteClearsFilledCurrentBoxAndMovesBack() {
        launch()
        paste("123456")
        digit(5).performTextClearance()
        assertDigit(5, "")
        assertDigit(4, "5")
        digit(4).assertIsFocused()
    }

    @Test fun mixedPasteIsRejectedWithoutChangingExistingDigits() {
        launch()
        paste("123")
        paste("12a34b56")
        "123".forEachIndexed { index, char -> assertDigit(index, char.toString()) }
        (3..5).forEach { assertDigit(it, "") }
        rule.onNodeWithTag("message").assertTextEquals("Only numbers are accepted. Paste digits only.")
        rule.onNodeWithTag("submit").assertIsNotEnabled()
    }

    @Test fun rejectsLettersSpacesAndNonAsciiDigits() {
        launch()
        listOf("a", " ", "١", "12\n34", "-1").forEach {
            digit(0).performTextReplacement(it)
            assertDigit(0, "")
        }
        rule.onNodeWithTag("submit").assertIsNotEnabled()
    }

    @Test fun hardwareDeleteClearsCurrentAndMovesBackKeepingPrevious() {
        launch()
        paste("123456")
        digit(5).performKeyInput { pressKey(Key.Backspace) }
        assertDigit(5, "")
        assertDigit(4, "5")
        digit(4).assertIsFocused()
        digit(4).performKeyInput { pressKey(Key.Delete) }
        assertDigit(4, "")
        assertDigit(3, "4")
        digit(3).assertIsFocused()
        rule.onNodeWithTag("submit").assertIsNotEnabled()
    }

    @Test fun softwareDeleteFromEmptyBoxMovesBackAndFirstBoxStaysInBounds() {
        launch()
        digit(0).performTextInput("1")
        digit(1).performTextClearance()
        digit(0).assertIsFocused()
        assertDigit(0, "1")
        digit(0).performTextClearance()
        assertDigit(0, "")
        digit(0).assertIsFocused()
    }

    @Test fun tappingEarlierBoxAllowsReplacementAndPartialPaste() {
        launch()
        paste("123456")
        digit(1).performClick().performTextReplacement("9")
        assertDigit(0, "1")
        assertDigit(1, "9")
        digit(2).assertIsFocused()
        paste("78", index = 3)
        assertDigit(3, "7")
        assertDigit(4, "8")
        assertDigit(5, "6")
    }

    @Test fun firstSubmitStartsOneMinuteTimerAndResendRestartsIt() {
        launch()
        paste("123456")
        rule.onNodeWithTag("submit").performClick()
        rule.onNodeWithTag("countdown").assertTextEquals("Retry in 1:00")
        rule.onNodeWithTag("resend").assertIsNotEnabled()
        advance(30)
        rule.onNodeWithTag("submit").performClick()
        rule.onNodeWithTag("countdown").assertTextEquals("Retry in 0:30")
        advance(29)
        rule.onNodeWithTag("countdown").assertTextEquals("Retry in 0:01")
        rule.onNodeWithTag("resend").assertIsNotEnabled()
        advance(1)
        rule.onNodeWithTag("resend").assertIsEnabled().performClick()
        rule.onNodeWithTag("countdown").assertTextEquals("Retry in 1:00")
        rule.onNodeWithTag("resend").assertIsNotEnabled()
        rule.onNodeWithTag("submit").assertIsNotEnabled()
        repeat(6) { assertDigit(it, "") }
        digit(0).assertIsFocused()
    }

    @Test fun restoresDigitsAndCountdownWithoutRestartingTimer() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            clipboard = LocalClipboardManager.current
            HakkaTheme { OtpScreen(nowMillis = { now }) }
        }
        paste("123456")
        rule.onNodeWithTag("submit").performClick()
        advance(20)
        restoration.emulateSavedInstanceStateRestore()
        "123456".forEachIndexed { index, char -> assertDigit(index, char.toString()) }
        rule.onNodeWithTag("countdown").assertTextEquals("Retry in 0:40")
        rule.onNodeWithTag("resend").assertIsNotEnabled()
    }

    @Test fun darkThemeSupportsInputAndImeSubmission() {
        launch(dark = true)
        paste("654321")
        digit(5).performImeAction()
        rule.onNodeWithTag("message").assertTextEquals(
            "Code submitted. This is a demo; no verification request was sent.")
        rule.onNodeWithTag("countdown").assertTextEquals("Retry in 1:00")
    }
}
