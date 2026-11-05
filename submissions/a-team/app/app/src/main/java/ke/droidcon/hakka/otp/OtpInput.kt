package ke.droidcon.hakka.otp

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.semantics.clearAndSetSemantics
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

/** Numeric keypad for every digit box. */
internal val OtpKeyboardOptions = KeyboardOptions(
    keyboardType = KeyboardType.Number,
    imeAction = ImeAction.Done,
)

/** Shown in empty boxes. Purely decorative: it is never part of the entered code. */
private const val PLACEHOLDER = "•"

private val MaxBoxSize = 52.dp
private val BoxSpacing = 8.dp

fun digitTestTag(index: Int): String = "otp_digit_${index + 1}"

/**
 * Six circular single-digit fields. Stateless: it renders [state] and reports events;
 * focus follows [OtpUiState.focusedIndex], and the first box is focused on entry so the
 * numeric keyboard opens automatically.
 */
@Composable
fun OtpInput(
    state: OtpUiState,
    onInput: (index: Int, text: String) -> Unit,
    onBackspace: (index: Int) -> Unit,
    onFocus: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val focusRequesters = remember { List(OTP_LENGTH) { FocusRequester() } }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.focusedIndex) {
        focusRequesters[state.focusedIndex].requestFocus()
        keyboard?.show()
    }

    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val boxSize = min(MaxBoxSize, (maxWidth - BoxSpacing * (OTP_LENGTH - 1)) / OTP_LENGTH)
        Row(horizontalArrangement = Arrangement.spacedBy(BoxSpacing)) {
            repeat(OTP_LENGTH) { index ->
                DigitBox(
                    index = index,
                    digit = state.digits[index],
                    boxState = state.boxState(index),
                    enabled = enabled,
                    onInput = onInput,
                    onBackspace = onBackspace,
                    onFocus = onFocus,
                    modifier = Modifier
                        .size(boxSize)
                        .focusRequester(focusRequesters[index]),
                )
            }
        }
    }
}

@Composable
private fun DigitBox(
    index: Int,
    digit: Char?,
    boxState: DigitBoxState,
    enabled: Boolean,
    onInput: (Int, String) -> Unit,
    onBackspace: (Int) -> Unit,
    onFocus: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOtpColors.current
    val text = digit?.toString().orEmpty()
    val (background, outline, textColor) = when (boxState) {
        DigitBoxState.Filled -> Triple(colors.digitFilledBg, colors.digitFilledBg, colors.digitFilledText)
        DigitBoxState.Focused -> Triple(colors.digitFocusedBg, colors.digitFocusedOutline, colors.textPrimary)
        DigitBoxState.Empty -> Triple(colors.digitEmptyBg, colors.digitEmptyOutline, colors.digitPlaceholder)
    }

    BasicTextField(
        value = TextFieldValue(text, selection = TextRange(text.length)),
        onValueChange = { if (it.text != text) onInput(index, it.text) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = OtpKeyboardOptions,
        textStyle = TextStyle(
            color = textColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(colors.textPrimary),
        modifier = modifier
            .onFocusChanged { if (it.isFocused) onFocus(index) }
            .onPreviewKeyEvent { event ->
                val isBackspaceOnEmpty = event.type == KeyEventType.KeyDown &&
                    event.key == Key.Backspace && digit == null
                if (isBackspaceOnEmpty) onBackspace(index)
                isBackspaceOnEmpty
            }
            .testTag(digitTestTag(index))
            .semantics { contentDescription = digitContentDescription(index) },
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(background)
                    .border(if (boxState == DigitBoxState.Focused) 1.5.dp else 1.dp, outline, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (boxState == DigitBoxState.Empty) {
                    Text(
                        text = PLACEHOLDER,
                        color = colors.digitPlaceholder,
                        fontSize = 18.sp,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
                innerTextField()
            }
        },
    )
}
