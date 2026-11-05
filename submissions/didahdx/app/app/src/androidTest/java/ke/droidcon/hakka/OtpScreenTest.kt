package ke.droidcon.hakka

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Rule
import org.junit.Test

class OtpScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun digit(n: Int) = onView(withContentDescription("Digit $n of 6"))
    private fun enter(value: String) { digit(1).perform(click(), typeText(value)); compose.waitForIdle() }


    @Test fun rapidHardwareTypingDoesNotLoseDigitsBeforeRecomposition() {
        digit(1).perform(click())
        compose.runOnUiThread {
            for (code in android.view.KeyEvent.KEYCODE_1..android.view.KeyEvent.KEYCODE_6) {
                compose.activity.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, code))
                compose.activity.dispatchKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, code))
            }
        }
        compose.waitForIdle()
        "123456".forEachIndexed { index, c -> digit(index + 1).check(matches(withText(c.toString()))) }
        compose.onNodeWithTag("otp_submit").assertIsEnabled().performClick()
        compose.onNodeWithTag("otp_status").assertTextEquals("You're in.").assertIsDisplayed()
    }

    @Test fun typingDeletionAndSubmission() {
        compose.onNodeWithTag("otp_submit").assertIsNotEnabled()
        compose.onNodeWithTag("otp_resend").assertIsNotEnabled()
        digit(1).check(matches(hasFocus()))
        enter("12")
        digit(3).check(matches(hasFocus()))
        digit(3).perform(pressKey(android.view.KeyEvent.KEYCODE_DEL))
        digit(2).check(matches(withText(""))).check(matches(hasFocus()))
        digit(2).perform(typeText("23456"))
        compose.onNodeWithTag("otp_submit").assertIsEnabled().performClick()
        compose.onNodeWithTag("otp_status").assertTextEquals("You're in.").assertIsDisplayed()
    }

    @Test fun pastedTextAndInvalidCodeReset() {
        compose.runOnUiThread {
            val clipboard = compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("OTP", "Your code is 482913"))
        }
        digit(1).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints() = isAssignableFrom(DigitEditText::class.java)
            override fun getDescription() = "Paste mixed text using the real Android clipboard action"
            override fun perform(controller: androidx.test.espresso.UiController, view: android.view.View) {
                (view as DigitEditText).onTextContextMenuItem(android.R.id.paste)
                controller.loopMainThreadUntilIdle()
            }
        })
        "482913".forEachIndexed { index, c -> digit(index + 1).check(matches(withText(c.toString()))) }
        compose.onNodeWithTag("otp_submit").performClick()
        compose.onNodeWithTag("otp_status").assertTextEquals("That code didn't work. Try again.").assertIsDisplayed()
        (1..6).forEach { digit(it).check(matches(withText(""))) }
        digit(1).check(matches(hasFocus()))
        compose.onNodeWithTag("otp_submit").assertIsNotEnabled()
    }

    @Test fun imeBackspaceOnEmptyFieldAndActivityRecreation() {
        enter("21")
        digit(3).perform(object : androidx.test.espresso.ViewAction {
            override fun getConstraints() = isAssignableFrom(DigitEditText::class.java)
            override fun getDescription() = "Delete through the soft keyboard input connection"
            override fun perform(controller: androidx.test.espresso.UiController, view: android.view.View) {
                (view as DigitEditText).onCreateInputConnection(EditorInfo())!!.deleteSurroundingText(1, 0)
                controller.loopMainThreadUntilIdle()
            }
        })
        digit(2).check(matches(withText(""))).check(matches(hasFocus()))
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        digit(1).check(matches(withText("2")))
        digit(2).check(matches(hasFocus()))
    }
}
