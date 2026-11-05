package ke.droidcon.hakka.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Invisible zero-width character kept in front of each box's digit purely so a backspace on
 * an *empty* box still produces a text change we can detect (soft keyboards don't reliably
 * send key events for that). It never reaches [OtpState].
 */
private const val SENTINEL = "​"

/**
 * Six circular [BasicTextField]s bound to an [OtpState]. All rules live in [OtpState];
 * this composable only translates text changes and focus into state calls.
 * [state] is the latest value; [update] applies a change to it.
 */
@Composable
fun OtpInput(
    state: OtpState,
    /** Applies a transformation to the latest state (avoids stale-state races between callbacks). */
    update: ((OtpState) -> OtpState) -> Unit,
    colors: OtpColors,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
) {
    val focusRequesters = remember { List(OtpState.LENGTH) { FocusRequester() } }
    val hasFocus = remember { mutableStateListOf(*Array(OtpState.LENGTH) { false }) }
    val keyboard = LocalSoftwareKeyboardController.current

    // Drive real focus from the state (typing advances, backspace retreats, errors reset to 1).
    LaunchedEffect(state.focusedIndex, state.status) {
        focusRequesters[state.focusedIndex].requestFocus()
        keyboard?.show()
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        for (index in 0 until OtpState.LENGTH) {
            val digit = state.digits[index]
            val digitText = digit?.toString().orEmpty()
            val current = SENTINEL + digitText
            val focused = hasFocus[index]

            val (bg, outline, textColor) = when {
                focused -> Triple(colors.digitFocusedBg, colors.digitFocusedOutline, colors.textPrimary)
                digit != null -> Triple(colors.digitFilledBg, colors.digitFilledBg, colors.digitFilledText)
                else -> Triple(colors.digitEmptyBg, colors.digitEmptyOutline, colors.digitPlaceholder)
            }

            BasicTextField(
                value = TextFieldValue(current, selection = TextRange(current.length)),
                onValueChange = { new ->
                    val text = new.text
                    when {
                        text == current -> Unit // selection/composition change only
                        text.length < current.length -> update { it.backspace(index) }
                        else -> {
                            val added = when {
                                text.startsWith(current) -> text.removePrefix(current)
                                digit != null && text.endsWith(digitText) -> text.removeSuffix(digitText)
                                else -> text
                            }
                            update { it.input(index, added) }
                        }
                    }
                },
                singleLine = true,
                cursorBrush = SolidColor(Color.Transparent),
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = if (index == OtpState.LENGTH - 1) ImeAction.Done else ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { update { it.focus(index + 1) } },
                    onDone = { onDone() },
                ),
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 52.dp)
                    .aspectRatio(1f)
                    .focusRequester(focusRequesters[index])
                    .onFocusChanged {
                        hasFocus[index] = it.isFocused
                        if (it.isFocused) update { s -> s.focus(index) }
                    }
                    .testTag("otp_digit_${index + 1}")
                    .semantics {
                        contentDescription = "Digit ${index + 1} of ${OtpState.LENGTH}"
                        stateDescription = digitText.ifEmpty { "Empty" }
                    },
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bg, CircleShape)
                            .border(if (focused) 1.5.dp else 1.dp, outline, CircleShape),
                    ) {
                        if (digit == null) {
                            // Placeholder only: never part of the stored code.
                            Text(
                                text = "0",
                                color = colors.digitPlaceholder,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}
