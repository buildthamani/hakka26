package ke.droidcon.hakka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.droidcon.hakka.R
import ke.droidcon.hakka.domain.model.OTP_LENGTH
import ke.droidcon.hakka.presentation.OtpColors

@Composable
fun OtpDigitBox(
    index: Int,
    digit: String,
    focused: Boolean,
    colors: OtpColors,
    focusRequester: FocusRequester,
    onInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onFocused: () -> Unit,
    onPaste: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        digit.isNotEmpty() -> colors.digitFilledBg
        focused -> colors.digitFocusedBg
        else -> colors.digitEmptyBg
    }
    val outline = when {
        digit.isNotEmpty() -> colors.digitFilledBg
        focused -> colors.digitFocusedOutline
        else -> colors.digitEmptyOutline
    }
    val borderWidth = if (focused && digit.isEmpty()) 2.dp else 1.dp
    val digitAccessibility = stringResource(R.string.otp_digit_accessibility, index + 1, OTP_LENGTH)

    BasicTextField(
        value = TextFieldValue(digit, selection = TextRange(digit.length)),
        onValueChange = { value ->
            val newText = value.text
            when {
                newText.isEmpty() && digit.isNotEmpty() -> onBackspace()
                newText.length > 1 -> onPaste(newText)
                newText != digit -> onInput(newText.removePrefix(digit).ifEmpty { newText })
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = TextStyle(
            color = colors.digitFilledText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(colors.digitFocusedOutline),
        modifier = modifier
            .size(46.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace && digit.isEmpty()) {
                    onBackspace()
                    true
                } else {
                    false
                }
            }
            .testTag("otp_digit_${index + 1}")
            .semantics { contentDescription = digitAccessibility }
            .background(background, CircleShape)
            .border(borderWidth, outline, CircleShape),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (digit.isEmpty()) {
                    Text(
                        text = "–",
                        color = colors.digitPlaceholder,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                innerTextField()
            }
        },
    )
}
