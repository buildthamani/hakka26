package ke.droidcon.hakka

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DigitBox(
    index: Int,
    state: OtpState,
    colors: OtpColors,
    requester: FocusRequester,
    onInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onFocused: () -> Unit,
) {
    val digit = state.digits[index]
    var focused by remember { mutableStateOf(false) }
    val bg = when {
        focused -> colors.focusedBg
        digit != null -> colors.filledBg
        else -> colors.emptyBg
    }
    val border = when {
        focused -> BorderStroke(2.dp, colors.focusedOutline)
        digit != null -> null
        else -> BorderStroke(1.dp, colors.emptyOutline)
    }
    val textColor = if (focused) colors.textPrimary else colors.filledText

    BasicTextField(
        value = digit?.toString() ?: "",
        onValueChange = onInput,
        singleLine = true,
        textStyle = TextStyle(color = textColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg, CircleShape)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused() }
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.Backspace && digit == null) {
                    onBackspace(); true
                } else false
            }
            .testTag("otp_digit_${index + 1}")
            .semantics { contentDescription = "Digit ${index + 1} of ${OtpState.LENGTH}" },
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (digit == null && !focused) {
                    Text("•", color = colors.placeholder, fontSize = 20.sp)
                }
                inner()
            }
        },
    )
}

