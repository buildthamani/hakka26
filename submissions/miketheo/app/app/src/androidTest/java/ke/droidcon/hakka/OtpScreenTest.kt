package ke.droidcon.hakka

import android.view.inputmethod.InputConnection
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.material3.MaterialTheme
import java.util.concurrent.atomic.AtomicReference
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class OtpScreenTest {
    @get:Rule val compose = createComposeRule()
    private val inputConnection = AtomicReference<InputConnection?>()
    private fun launch(dark: Boolean = false) {
        compose.setContent {
            val interceptor = remember {
                PlatformTextInputInterceptor { request, next ->
                    next.startInputMethod(PlatformTextInputMethodRequest { attributes ->
                        request.createInputConnection(attributes).also { inputConnection.set(it) }
                    })
                }
            }
            InterceptPlatformTextInput(interceptor) {
                MaterialTheme { OtpScreen(dark = dark) }
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasTestTag("otp_digit_1") and isFocused())
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
    @Test fun initialFocusLabelsAndDisabledActions() {
        launch()
        compose.onNodeWithTag("otp_digit_1").assertIsFocused()
        for (i in 1..6) compose.onNodeWithTag("otp_digit_$i")
            .assertContentDescriptionEquals("Digit $i of 6")
        compose.onNodeWithTag("otp_submit").assertIsNotEnabled()
        compose.onNodeWithTag("otp_resend").assertIsNotEnabled()
    }
    @Test fun typingAndEmptyBackspaceMoveFocus() {
        launch()
        compose.onNodeWithTag("otp_digit_1").performTextInput("4")
        compose.onNodeWithTag("otp_digit_2").assertIsFocused()
            .performKeyInput { pressKey(Key.Backspace) }
        compose.onNodeWithTag("otp_digit_1").assertIsFocused()
        compose.onNodeWithTag("otp_submit").assertIsNotEnabled()
    }
    @Test fun softwareKeyboardBackspaceOnEmptyBoxClearsPreviousDigit() {
        launch()
        val firstConnection = inputConnection.get()
        compose.onNodeWithTag("otp_digit_1").performTextInput("4")
        compose.onNodeWithTag("otp_digit_2").assertIsFocused()
        compose.waitUntil(timeoutMillis = 5_000) {
            inputConnection.get() != null && inputConnection.get() !== firstConnection
        }
        compose.runOnIdle { inputConnection.get()!!.deleteSurroundingText(1, 0) }
        compose.onNodeWithTag("otp_digit_1").assertIsFocused().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")),
        )
        compose.onNodeWithTag("otp_submit").assertIsNotEnabled()
    }
    @Test fun tappingEarlierDigitReplacesItAndAdvances() {
        launch()
        compose.onNodeWithTag("otp_digit_1").performTextInput("123")
        compose.onNodeWithTag("otp_digit_2").performClick().performTextInput("9")
        compose.onNodeWithTag("otp_digit_2").assertTextEquals("9")
        compose.onNodeWithTag("otp_digit_3").assertIsFocused()
    }
    @Test fun halfwayInputAdvancesToEmptyFourthCircle() {
        launch()
        compose.onNodeWithTag("otp_digit_1").performTextInput("123")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasTestTag("otp_digit_4") and isFocused())
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("otp_digit_4").assertIsFocused()
        compose.onNodeWithTag("otp_digit_3").assertIsNotFocused()
        compose.onNodeWithTag("otp_submit").assertIsNotEnabled()
    }
    @Test fun pastedWrongCodeClearsAndShowsExactError() {
        launch()
        compose.onNodeWithTag("otp_digit_1").performTextInput("Your code is 482913")
        compose.onNodeWithTag("otp_submit").assertIsEnabled().performScrollTo().performClick()
        compose.onNodeWithTag("otp_status").assertTextEquals("That code didn't work. Try again.")
        compose.onNodeWithTag("otp_digit_1").assertIsFocused()
        compose.onNodeWithTag("otp_submit").assertIsNotEnabled()
    }
    @Test fun correctCodeSucceedsInDarkMode() {
        launch(dark = true)
        compose.onNodeWithTag("otp_digit_1").performTextInput("123456")
        compose.onNodeWithTag("otp_submit").performScrollTo().performClick()
        compose.onNodeWithTag("otp_status").assertTextEquals("You're in.")
    }
}
