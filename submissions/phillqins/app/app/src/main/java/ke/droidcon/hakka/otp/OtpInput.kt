package ke.droidcon.hakka.otp

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.unit.sp
import ke.droidcon.hakka.R
import ke.droidcon.hakka.ui.theme.LocalOtpColors

private const val PLACEHOLDER = "-"

/**
 * A row of [OTP_LENGTH] circular digit boxes. Stateless: [digits] and [focusedIndex] come from
 * the caller, and every edit is reported back through the callbacks. Edit callbacks return the
 * box to focus next, which is focused straight away so fast typing never lands in a stale box.
 */
@Composable
fun OtpInput(
    digits: List<String>,
    focusedIndex: Int,
    onBoxTextChange: (index: Int, text: String) -> Int,
    onBackspaceOnEmptyBox: (index: Int) -> Int,
    onBoxFocused: (index: Int) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequesters = remember { List(OTP_LENGTH) { FocusRequester() } }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(focusedIndex) {
        focusRequesters[focusedIndex].requestFocus()
        keyboard?.show()
    }

    Row(
        modifier = modifier.fillMaxWidth().widthIn(max = 380.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        digits.forEachIndexed { index, digit ->
            OtpDigitBox(
                index = index,
                digit = digit,
                focusRequester = focusRequesters[index],
                onTextChange = { focusRequesters[onBoxTextChange(index, it)].requestFocus() },
                onBackspaceOnEmpty = { focusRequesters[onBackspaceOnEmptyBox(index)].requestFocus() },
                onFocused = { onBoxFocused(index) },
                onDone = onDone,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OtpDigitBox(
    index: Int,
    digit: String,
    focusRequester: FocusRequester,
    onTextChange: (String) -> Unit,
    onBackspaceOnEmpty: () -> Unit,
    onFocused: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOtpColors.current
    var isFocused by remember { mutableStateOf(false) }
    val description = stringResource(R.string.otp_digit_description, index + 1, OTP_LENGTH)
    val isLast = index == OTP_LENGTH - 1

    val background: Color
    val border: BorderStroke
    val textColor: Color
    when {
        isFocused -> {
            background = colors.digitFocusedBg
            border = BorderStroke(1.5.dp, colors.digitFocusedOutline)
            textColor = colors.textPrimary
        }
        digit.isNotEmpty() -> {
            background = colors.digitFilledBg
            border = BorderStroke(1.dp, colors.digitFilledBg)
            textColor = colors.digitFilledText
        }
        else -> {
            background = colors.digitEmptyBg
            border = BorderStroke(1.dp, colors.digitEmptyOutline)
            textColor = colors.digitPlaceholder
        }
    }
    val textStyle = TextStyle(
        color = textColor,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
    )

    BasicTextField(
        value = TextFieldValue(digit, selection = TextRange(digit.length)),
        onValueChange = { onTextChange(it.text) },
        modifier = modifier
            .aspectRatio(1f)
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                val isBackspaceOnEmpty = event.type == KeyEventType.KeyDown &&
                    event.key == Key.Backspace &&
                    digit.isEmpty()
                if (isBackspaceOnEmpty) onBackspaceOnEmpty()
                isBackspaceOnEmpty
            }
            .semantics { contentDescription = description }
            .testTag("otp_digit_${index + 1}"),
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = if (isLast) ImeAction.Done else ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(background)
                    .border(border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (digit.isEmpty()) Text(PLACEHOLDER, style = textStyle)
                innerTextField()
            }
        },
    )
}
