package ke.droidcon.hakka.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val PLACEHOLDER = "•"

/**
 * One invisible text field that holds the whole code, drawn as six circles. The field's text is always the
 * current [code] with the cursor at its end, so the circles are a pure rendering of the code: box `i` is
 * filled when `i < code.length` and [focusedIndex] (the first empty box) is drawn as focused while the field
 * has focus. Taps and long presses on the circles reach the field underneath, so the numeric keyboard and
 * the paste menu work as usual.
 */
@Composable
fun OtpCodeInput(
    code: String,
    focusedIndex: Int?,
    onCodeChange: (String) -> Unit,
    onDone: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var hasFocus by remember { mutableStateOf(false) }
    val hiddenSelection = remember {
        TextSelectionColors(handleColor = Color.Transparent, backgroundColor = Color.Transparent)
    }

    CompositionLocalProvider(LocalTextSelectionColors provides hiddenSelection) {
        BasicTextField(
            value = TextFieldValue(text = code, selection = TextRange(code.length)),
            onValueChange = { onCodeChange(it.text) },
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged { hasFocus = it.isFocused }
                .semantics { contentDescription = "Verification code" },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            decorationBox = { innerTextField ->
                Box {
                    OtpDigitRow(code = code, focusedIndex = if (hasFocus) focusedIndex else null)
                    Box(Modifier.matchParentSize().alpha(0f)) { innerTextField() }
                }
            },
        )
    }
}

/** Six circles, one per digit of the code, with the box at [focusedIndex] (if any) drawn as focused. */
@Composable
fun OtpDigitRow(code: String, focusedIndex: Int?, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(CODE_LENGTH) { index ->
            OtpDigitBox(
                digit = code.getOrNull(index),
                focused = index == focusedIndex,
                modifier = Modifier
                    .testTag("otp_digit_${index + 1}")
                    .semantics { contentDescription = "Digit ${index + 1} of $CODE_LENGTH" },
            )
        }
    }
}

@Composable
private fun OtpDigitBox(digit: Char?, focused: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalOtpColors.current
    val (background, outline, outlineWidth) = when {
        digit != null -> Triple(colors.digitFilledBg, colors.digitFilledBg, 1.dp)
        focused -> Triple(colors.digitFocusedBg, colors.digitFocusedOutline, 2.dp)
        else -> Triple(colors.digitEmptyBg, colors.digitEmptyOutline, 1.dp)
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .background(background, CircleShape)
            .border(outlineWidth, outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = digit?.toString() ?: PLACEHOLDER,
            color = if (digit != null) colors.digitFilledText else colors.digitPlaceholder,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
