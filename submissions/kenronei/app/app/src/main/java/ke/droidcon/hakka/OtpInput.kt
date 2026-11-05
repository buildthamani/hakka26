package ke.droidcon.hakka

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp

/**
 * Six circular digit boxes. Stateless: renders [state] and reports events; the caller
 * applies them to the latest [OtpState].
 */
@Composable
fun OtpInput(
    state: OtpState,
    onInput: (index: Int, raw: String) -> Unit,
    onBackspace: (index: Int) -> Unit,
    onFocused: (index: Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val requesters = remember { List(OtpState.LENGTH) { FocusRequester() } }
    val keyboard = LocalSoftwareKeyboardController.current

    // Model-driven focus moves (start, auto-advance, backspace, error reset).
    LaunchedEffect(state.focusRequest) {
        withFrameNanos { }
        runCatching { requesters[state.focusedIndex].requestFocus() }
        keyboard?.show()
    }

    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val gap = 10.dp
        val boxSize = min(52.dp, (maxWidth - gap * (OtpState.LENGTH - 1)) / OtpState.LENGTH)
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            state.digits.forEachIndexed { index, digit ->
                DigitBox(
                    digit = digit,
                    index = index,
                    focused = state.focusedIndex == index,
                    requester = requesters[index],
                    onValueChange = { onInput(index, it) },
                    onBackspace = { onBackspace(index) },
                    onFocused = { onFocused(index) },
                    onDone = onDone,
                    modifier = Modifier.size(boxSize),
                )
            }
        }
    }
}

@Composable
private fun DigitBox(
    digit: String,
    index: Int,
    focused: Boolean,
    requester: FocusRequester,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit,
    onFocused: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOtpColors.current
    val filled = digit.isNotEmpty()
    val background = when {
        filled -> colors.digitFilledBg
        focused -> colors.digitFocusedBg
        else -> colors.digitEmptyBg
    }
    val outline = when {
        focused -> colors.digitFocusedOutline
        filled -> colors.digitFilledBg
        else -> colors.digitEmptyOutline
    }
    val label = OtpCopy.digitLabel(index)

    BasicTextField(
        // Cursor pinned to the end so a newly typed digit is always the last character.
        value = TextFieldValue(digit, selection = TextRange(digit.length)),
        onValueChange = { if (it.text != digit) onValueChange(it.text) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        textStyle = TextStyle(
            color = if (filled) colors.digitFilledText else colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(Color.Transparent),
        modifier = modifier
            .focusRequester(requester)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Backspace && digit.isEmpty()) {
                    onBackspace()
                    true
                } else {
                    false
                }
            }
            .testTag("otp_digit_${index + 1}")
            .semantics { contentDescription = label },
        decorationBox = { inner ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(background, CircleShape)
                    .border(if (focused) 2.dp else 1.dp, outline, CircleShape),
            ) {
                if (!filled) {
                    Text(
                        text = OtpCopy.PLACEHOLDER,
                        color = colors.digitPlaceholder,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                inner()
            }
        },
    )
}
