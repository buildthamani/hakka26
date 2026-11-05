package ke.droidcon.hakka

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OtpInput(state: OtpState, colors: OtpColors, onChange: ((OtpState) -> OtpState) -> Unit, onSubmit: () -> Unit) {
    val requesters = remember { List(6) { FocusRequester() } }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.active, state.focusRequest) {
        requesters[state.active].requestFocus()
    }
    LaunchedEffect(Unit) { keyboard?.show() }
    CompositionLocalProvider(LocalTextSelectionColors provides TextSelectionColors(
        handleColor = colors.outline, backgroundColor = Color.Transparent,
    )) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(6) { index ->
                val digit = state.digits[index]
                val value = digit
                val focused = state.active == index
                val background = when {
                    digit.isNotEmpty() -> colors.filled
                    focused -> colors.focused
                    else -> colors.empty
                }
                EmptyFieldBackspace(
                    enabled = focused && digit.isEmpty(),
                    onBackspace = { onChange { it.delete(index) } },
                ) {
                    BasicTextField(
                        value = TextFieldValue(value, if (index == 5 && digit.isNotEmpty())
                            TextRange(value.length) else TextRange(0, value.length)),
                        onValueChange = { edit ->
                            if (edit.text != digit) {
                                if (edit.text.isEmpty()) onChange { it.delete(index) }
                                else onChange { it.input(index, edit.text) }
                            }
                        },
                        modifier = Modifier.weight(1f).aspectRatio(1f).testTag("otp_digit_${index + 1}")
                            .focusRequester(requesters[index])
                            .onFocusChanged { if (it.isFocused)
                                onChange { current -> current.copy(active = index) } }
                            .onPreviewKeyEvent {
                                if (it.key == Key.Backspace || it.key == Key.Delete) {
                                    if (it.type == KeyEventType.KeyDown) onChange { it.delete(index) }
                                    true
                                } else false
                            }
                            .semantics {
                                contentDescription = "Digit ${index + 1} of 6"
                                selected = focused
                                stateDescription = when {
                                    digit.isNotEmpty() -> "Filled"
                                    focused -> "Focused, empty"
                                    else -> "Empty"
                                }
                            },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number,
                            imeAction = if (index == 5) ImeAction.Done else ImeAction.Next),
                        keyboardActions = KeyboardActions(onDone = { onSubmit() },
                            onNext = { onChange { it.copy(active = (index + 1).coerceAtMost(5)) } }),
                        cursorBrush = SolidColor(Color.Transparent),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center, color = colors.filledText),
                        decorationBox = { inner ->
                            Surface(shape = CircleShape, color = background,
                                border = BorderStroke(1.dp, when {
                                    digit.isNotEmpty() -> colors.filled
                                    focused -> colors.outline
                                    else -> colors.emptyOutline
                                })) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (digit.isEmpty()) Text("–", color = colors.placeholder,
                                        modifier = Modifier.clearAndSetSemantics {})
                                    inner()
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}
