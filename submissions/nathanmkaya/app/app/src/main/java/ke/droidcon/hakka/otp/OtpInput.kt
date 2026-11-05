package ke.droidcon.hakka.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.droidcon.hakka.R

/**
 * Six circular digit boxes. The caller owns the digits and which box should hold focus;
 * this component reports raw input, backspaces and focus changes per box.
 */
@Composable
fun OtpInput(
    digits: List<Char?>,
    focusedIndex: Int,
    onInput: (index: Int, text: String) -> Unit,
    onBackspace: (index: Int) -> Unit,
    onFocus: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequesters = remember(digits.size) { List(digits.size) { FocusRequester() } }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusedIndex) {
        focusRequesters[focusedIndex].requestFocus()
        keyboard?.show()
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        digits.forEachIndexed { index, digit ->
            DigitBox(
                digit = digit,
                position = index + 1,
                onInput = { onInput(index, it) },
                onBackspace = { onBackspace(index) },
                onFocused = { onFocus(index) },
                modifier = Modifier.weight(1f).focusRequester(focusRequesters[index]),
            )
        }
    }
}

private data class DigitBoxStyle(val background: Color, val outline: Color, val outlineWidth: Dp, val content: Color)

private fun OtpColors.digitBoxStyle(digit: Char?, isFocused: Boolean): DigitBoxStyle = when {
    digit != null -> DigitBoxStyle(digitFilledBg, digitFilledBg, 1.dp, digitFilledText)
    isFocused -> DigitBoxStyle(digitFocusedBg, digitFocusedOutline, 1.5.dp, textPrimary)
    else -> DigitBoxStyle(digitEmptyBg, digitEmptyOutline, 1.dp, digitPlaceholder)
}

@Composable
private fun DigitBox(
    digit: Char?,
    position: Int,
    onInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val style = LocalOtpColors.current.digitBoxStyle(digit, isFocused)
    val label = stringResource(R.string.otp_digit_label, position, OTP_LENGTH)
    val shown = digit?.toString() ?: if (isFocused) "" else "–"

    // The field's own text stays empty, so typing and pasting always arrive as fresh input
    // and every backspace arrives as a key event. The reducer alone decides what a box holds.
    BasicTextField(
        value = "",
        onValueChange = onInput,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(if (digit == null) style.content else Color.Transparent),
        modifier = modifier
            .aspectRatio(1f)
            .testTag("otp_digit_$position")
            .semantics {
                contentDescription = label
                digit?.let { stateDescription = it.toString() }
            }
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                val isBackspace = event.type == KeyEventType.KeyDown && event.key == Key.Backspace
                if (isBackspace) onBackspace()
                isBackspace
            },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(style.background, CircleShape)
                    .border(style.outlineWidth, style.outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = shown, color = style.content, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                innerTextField()
            }
        },
    )
}
