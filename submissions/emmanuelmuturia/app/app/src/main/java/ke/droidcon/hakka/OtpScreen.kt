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
import androidx.compose.foundation.layout.Spacer
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
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
        ) {
            Column(
                Modifier.navigationBarsPadding().imePadding().padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painterResource(if (isSystemInDarkTheme()) R.drawable.thamani_logo_white else R.drawable.thamani_logo), "Thamani logo", Modifier.size(56.dp))
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
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

                Spacer(Modifier.height(96.dp))
                Button(
                    onClick = { state = state.submit() },
                    enabled = state.canSubmit,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.buttonBg, contentColor = c.buttonText,
                        disabledContainerColor = c.buttonDisabledBg, disabledContentColor = c.textSecondary,
                    ),
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("otp_submit"),
                ) { Text("Submit", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Preview
@Composable
private fun OtpScreenPreview() {
    HakkaTheme { OtpScreen() }
}
