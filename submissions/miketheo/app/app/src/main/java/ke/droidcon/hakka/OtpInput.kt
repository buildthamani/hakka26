package ke.droidcon.hakka

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// A hidden editing anchor lets software keyboards report deletion even in an empty box.
private const val Anchor = "\u200B"
private val HideAnchor = VisualTransformation { value ->
    TransformedText(AnnotatedString(value.text.removePrefix(Anchor)), object : OffsetMapping {
        override fun originalToTransformed(offset: Int) = (offset - 1).coerceAtLeast(0)
        override fun transformedToOriginal(offset: Int) = (offset + 1).coerceAtMost(value.length)
    })
}

/**
 * Six digit input with keyboard focus owned by the reducer's requested position.
 * Focus callbacks only update the visual state; they cannot undo auto-advance.
 */
@Composable
fun OtpInput(
    digits: List<String>,
    focus: Int,
    status: String,
    focusRequest: Int,
    palette: OtpPalette,
    onChange: (OtpState) -> Unit,
) {
    val requesters = remember { List(6) { FocusRequester() } }
    var activeFocus by remember { mutableIntStateOf(focus) }
    val keyboard = LocalSoftwareKeyboardController.current
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(focusRequest, windowFocused) {
        if (windowFocused) {
            withFrameNanos { }
            requesters[focus].requestFocus()
            keyboard?.show()
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val diameter = ((maxWidth - 30.dp) / 6).coerceAtMost(52.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            repeat(6) { index ->
                val filled = digits[index].isNotEmpty()
                val selected = activeFocus == index && !filled
                val background = if (filled) palette.filled else if (selected) palette.sheet else palette.empty
                BasicTextField(
                    value = TextFieldValue(Anchor + digits[index], TextRange(1 + digits[index].length)),
                    onValueChange = { value ->
                        val state = OtpState(digits, focus, status)
                        when {
                            value.text.isEmpty() || (filled && value.text == Anchor) -> onChange(state.backspace(index))
                            value.text.filter { it in '0'..'9' }.isNotEmpty() -> {
                                val raw = value.text.removePrefix(Anchor)
                                // Appending to an existing digit replaces it; full pasted codes start at box one.
                                val input = if (filled && raw.length == 2 && raw.startsWith(digits[index])) raw.takeLast(1) else raw
                                onChange(state.input(index, input))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    visualTransformation = HideAnchor,
                    cursorBrush = SolidColor(Color.Transparent),
                    textStyle = TextStyle(color = if (filled) Color.White else palette.primary, fontSize = 20.sp, textAlign = TextAlign.Center),
                    modifier = Modifier.size(diameter).focusRequester(requesters[index])
                        .onFocusChanged { if (it.isFocused) activeFocus = index }
                        .onPreviewKeyEvent {
                            if (it.type == KeyEventType.KeyDown && it.key == Key.Backspace) {
                                onChange(OtpState(digits, focus, status).backspace(index)); true
                            } else false
                        }
                        .testTag("otp_digit_${index + 1}")
                        .semantics { contentDescription = "Digit ${index + 1} of 6" }
                        .background(background, CircleShape)
                        .border(if (selected || !filled) 1.dp else 0.dp,
                            if (selected) palette.primary else if (!filled) palette.outline else background, CircleShape),
                    decorationBox = { field ->
                        Box(contentAlignment = Alignment.Center) {
                            if (!filled) Text("•", color = palette.placeholder, fontSize = 20.sp)
                            field()
                        }
                    },
                )
            }
        }
    }
}
