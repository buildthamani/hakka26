package ke.droidcon.hakka.ui.otp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.droidcon.hakka.R
import ke.droidcon.hakka.ui.theme.HakkaTheme

private enum class DigitBoxState { Filled, Focused, Empty }

/** Visual hint shown in unfilled boxes, never part of the entered code. */
private const val PLACEHOLDER = "•"

/**
 * Six circular digit boxes driven by one hidden [BasicTextField], so the system keyboard,
 * backspace and paste all go through a single text value. Focus is always the next empty box.
 */
@Composable
fun OtpInput(
    code: String,
    length: Int,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(code.isEmpty()) {
        if (code.isEmpty()) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    BasicTextField(
        // Cursor pinned to the end: backspace always removes the previous digit.
        value = TextFieldValue(code, selection = TextRange(code.length)),
        onValueChange = { onCodeChange(it.text) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        modifier = modifier.focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.alpha(0f)) { innerTextField() }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(length) { index ->
                        val state = when {
                            index < code.length -> DigitBoxState.Filled
                            index == code.length -> DigitBoxState.Focused
                            else -> DigitBoxState.Empty
                        }
                        DigitBox(
                            digit = code.getOrNull(index),
                            state = state,
                            label = stringResource(R.string.otp_digit_label, index + 1, length),
                            tag = "otp_digit_${index + 1}",
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun DigitBox(digit: Char?, state: DigitBoxState, label: String, tag: String) {
    val colors = HakkaTheme.otpColors
    val (background, outline) = when (state) {
        DigitBoxState.Filled -> colors.digitFilledBg to colors.digitFilledBg
        DigitBoxState.Focused -> colors.digitFocusedBg to colors.digitFocusedOutline
        DigitBoxState.Empty -> colors.digitEmptyBg to colors.digitEmptyOutline
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(46.dp)
            .background(background, CircleShape)
            .border(if (state == DigitBoxState.Focused) 1.5.dp else 1.dp, outline, CircleShape)
            // Announce only "Digit N of 6" (+ the digit), never the placeholder.
            .clearAndSetSemantics {
                testTag = tag
                contentDescription = label
                if (digit != null) stateDescription = digit.toString()
            },
    ) {
        Text(
            text = digit?.toString() ?: PLACEHOLDER,
            color = if (digit != null) colors.digitFilledText else colors.digitPlaceholder,
            fontSize = 18.sp,
        )
    }
}
