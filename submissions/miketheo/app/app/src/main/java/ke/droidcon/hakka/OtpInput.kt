package ke.droidcon.hakka

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Six independently accessible fields; the reducer owns the requested focus. */
@Composable
fun OtpInput(state: OtpState, palette: OtpPalette, onChange: (OtpState) -> Unit) {
    val requesters = remember { List(6) { FocusRequester() } }
    val keyboard = LocalSoftwareKeyboardController.current
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(state.focus, state.digits, windowFocused) {
        if (windowFocused) {
            withFrameNanos { }
            requesters[state.focus].requestFocus()
            keyboard?.show()
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val diameter = ((maxWidth - 30.dp) / 6).coerceAtMost(52.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            repeat(6) { index ->
                OtpDigit(
                    digit = state.digits[index], index = index, diameter = diameter,
                    requester = requesters[index], palette = palette,
                    onInput = { onChange(state.input(index, it)) },
                    onBackspace = { onChange(state.backspace(index)) },
                )
            }
        }
    }
}

@Composable
private fun OtpDigit(
    digit: String,
    index: Int,
    diameter: androidx.compose.ui.unit.Dp,
    requester: FocusRequester,
    palette: OtpPalette,
    onInput: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val filled = digit.isNotEmpty()
    val selected = focused && !filled
    val background = when {
        filled -> palette.filled
        selected -> palette.sheet
        else -> palette.empty
    }
    EmptyBackspaceInput(empty = !filled, onBackspace = onBackspace) {
        BasicTextField(
            value = TextFieldValue(digit, TextRange(digit.length)),
            onValueChange = { value ->
                if (value.text != digit) {
                    if (filled && value.text.isEmpty()) {
                        onBackspace()
                    } else {
                        val input = when {
                            filled && value.text.length == 2 && value.text.startsWith(digit) -> value.text.takeLast(1)
                            filled && value.text.length == 2 && value.text.endsWith(digit) -> value.text.take(1)
                            else -> value.text
                        }
                        onInput(input)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            singleLine = true,
            interactionSource = interactions,
            cursorBrush = SolidColor(Color.Transparent),
            textStyle = TextStyle(
                color = if (filled) Color.White else palette.primary,
                fontSize = 20.sp, textAlign = TextAlign.Center,
            ),
            modifier = Modifier.size(diameter).focusRequester(requester)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Backspace) {
                        onBackspace()
                        true
                    } else false
                }
                .testTag("otp_digit_${index + 1}")
                .semantics { contentDescription = "Digit ${index + 1} of 6" }
                .background(background, CircleShape)
                .border(
                    if (filled) 0.dp else 1.dp,
                    if (selected) palette.primary else if (!filled) palette.outline else background,
                    CircleShape,
                ),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.Center) {
                    if (!filled) Text("•", color = palette.placeholder, fontSize = 20.sp)
                    field()
                }
            },
        )
    }
}
