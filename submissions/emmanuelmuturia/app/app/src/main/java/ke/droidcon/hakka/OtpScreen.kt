package ke.droidcon.hakka

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Palette from assets/colours.json ("otp" block). */
private class OtpColors(
    val sheet: Color, val scrim: Color, val textPrimary: Color, val textSecondary: Color,
    val filledBg: Color, val filledText: Color, val focusedBg: Color, val focusedOutline: Color,
    val emptyBg: Color, val emptyOutline: Color, val placeholder: Color,
    val buttonBg: Color, val buttonText: Color, val buttonDisabledBg: Color,
    val link: Color, val success: Color, val error: Color,
)

private val LightColors = OtpColors(
    Color(0xFFFFFFFF), Color(0x99000000), Color(0xFF111111), Color(0xFF5F6368),
    Color(0xFF12361B), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF111111),
    Color(0xFFF1F1F1), Color(0xFFE0E0E0), Color(0xFF9E9E9E),
    Color(0xFF12361B), Color(0xFFFFFFFF), Color(0xFFC9D1CB),
    Color(0xFF12361B), Color(0xFF1B7F3A), Color(0xFFC62828),
)

private val DarkColors = OtpColors(
    Color(0xFF161A17), Color(0xCC000000), Color(0xFFF5F5F5), Color(0xFFB0B5B1),
    Color(0xFF2E7D45), Color(0xFFFFFFFF), Color(0xFF161A17), Color(0xFFF5F5F5),
    Color(0xFF262B27), Color(0xFF353B36), Color(0xFF6F7570),
    Color(0xFF2E7D45), Color(0xFFFFFFFF), Color(0xFF2A302B),
    Color(0xFF7BC795), Color(0xFF7BC795), Color(0xFFEF9A9A),
)

@Composable
fun OtpScreen(modifier: Modifier = Modifier) {
    val c = if (isSystemInDarkTheme()) DarkColors else LightColors
    var state by remember { mutableStateOf(OtpState()) }
    val requesters = remember { List(OtpState.LENGTH) { FocusRequester() } }

    // Countdown for "Resend in 0:SS".
    LaunchedEffect(state.canResend, state.resendSeconds == OtpState.RESEND_SECONDS) {
        while (!state.canResend) {
            delay(1000)
            state = state.tick()
        }
    }
    // Keep real focus in sync with state.focusedIndex.
    LaunchedEffect(state.focusedIndex, state.status) {
        runCatching { requesters[state.focusedIndex].requestFocus() }
    }

    Box(modifier.fillMaxSize().background(c.scrim)) {
        Surface(
            color = c.sheet,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            Column(
                Modifier.navigationBarsPadding().imePadding().padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painterResource(R.drawable.thamani_logo), "Thamani logo", Modifier.size(48.dp))
                Text(
                    "Enter your code", color = c.textPrimary, fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    "A 6-digit code has been sent to your phone.", color = c.textSecondary,
                    fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
                )

                Row(
                    Modifier.padding(top = 24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    repeat(OtpState.LENGTH) { i ->
                        DigitBox(
                            index = i, state = state, colors = c, requester = requesters[i],
                            onInput = { state = state.onInput(i, it) },
                            onBackspace = { state = state.onBackspace(i) },
                            onFocused = { if (state.focusedIndex != i) state = state.focus(i) },
                        )
                    }
                }

                Text(
                    state.statusMessage ?: "",
                    color = if (state.status == OtpStatus.Success) c.success else c.error,
                    fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth().testTag("otp_status")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )

                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Didn't receive the code? ", color = c.textSecondary, fontSize = 14.sp)
                    Text(
                        state.resendLabel,
                        color = if (state.canResend) c.link else c.textSecondary,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("otp_resend").clickable(
                            enabled = state.canResend,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { state = state.resend() },
                    )
                }

                Button(
                    onClick = { state = state.submit() },
                    enabled = state.canSubmit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.buttonBg, contentColor = c.buttonText,
                        disabledContainerColor = c.buttonDisabledBg, disabledContentColor = c.textSecondary,
                    ),
                    modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(52.dp).testTag("otp_submit"),
                ) { Text("Submit", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun DigitBox(
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
            .size(46.dp)
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

@Preview
@Composable
private fun OtpScreenPreview() {
    HakkaTheme { OtpScreen() }
}
