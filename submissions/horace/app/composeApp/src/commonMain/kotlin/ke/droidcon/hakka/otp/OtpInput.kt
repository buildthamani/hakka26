package ke.droidcon.hakka.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import ke.droidcon.hakka.theme.OtpColors

private enum class DigitBoxState { Filled, Focused, Empty }

private const val PLACEHOLDER = "–"
private val BoxSpacing = 10.dp
private val MaxBoxSize = 52.dp

/** Six circular digit boxes driven entirely by [state]. */
@Composable
fun OtpInput(
    state: OtpState,
    colors: OtpColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val focusRequesters = remember(state.length) { List(state.length) { FocusRequester() } }
    val keyboard = LocalSoftwareKeyboardController.current

    // Follow focus moves made by the state; the first run autofocuses box 1 and opens the numpad.
    LaunchedEffect(state.focusRequest) {
        focusRequesters[state.focusedIndex].requestFocus()
        keyboard?.show()
    }

    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val boxSize: Dp = min(MaxBoxSize, (maxWidth - BoxSpacing * (state.length - 1)) / state.length)
        Row(horizontalArrangement = Arrangement.spacedBy(BoxSpacing)) {
            for (index in 0 until state.length) {
                val digit = state.digits[index]
                OtpDigitBox(
                    digit = digit,
                    visualState = when {
                        enabled && index == state.focusedIndex -> DigitBoxState.Focused
                        digit.isNotEmpty() -> DigitBoxState.Filled
                        else -> DigitBoxState.Empty
                    },
                    colors = colors,
                    enabled = enabled,
                    size = boxSize,
                    modifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .onFocusChanged { if (it.isFocused) state.onBoxFocused(index) }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace) {
                                state.backspace(index)
                                true
                            } else {
                                false
                            }
                        }
                        .testTag("otp_digit_${index + 1}")
                        .semantics { contentDescription = OtpCopy.digitLabel(index, state.length) },
                    onTextChange = { newText ->
                        if (newText.isEmpty()) state.backspace(index)
                        else state.enter(index, insertedText(digit, newText))
                    },
                )
            }
        }
    }
}

@Composable
private fun OtpDigitBox(
    digit: String,
    visualState: DigitBoxState,
    colors: OtpColors,
    enabled: Boolean,
    size: Dp,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (background, outline, textColor) = when (visualState) {
        DigitBoxState.Filled -> Triple(colors.digitFilledBg, colors.digitFilledBg, colors.digitFilledText)
        DigitBoxState.Focused -> Triple(colors.digitFocusedBg, colors.digitFocusedOutline, colors.textPrimary)
        DigitBoxState.Empty -> Triple(colors.digitEmptyBg, colors.digitEmptyOutline, colors.textPrimary)
    }
    val outlineWidth = if (visualState == DigitBoxState.Focused) 1.5.dp else 1.dp
    val textStyle = TextStyle(
        color = textColor,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )

    BasicTextField(
        // Cursor kept at the end so new input is appended after the existing digit.
        value = TextFieldValue(digit, selection = TextRange(digit.length)),
        onValueChange = { onTextChange(it.text) },
        enabled = enabled,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(textColor.copy(alpha = 0f)),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size)
                    .background(background, CircleShape)
                    .border(outlineWidth, outline, CircleShape),
            ) {
                if (digit.isEmpty()) {
                    Text(PLACEHOLDER, style = textStyle.copy(color = colors.digitPlaceholder))
                }
                innerTextField()
            }
        },
    )
}
