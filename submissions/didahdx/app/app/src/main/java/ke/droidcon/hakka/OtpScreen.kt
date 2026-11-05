package ke.droidcon.hakka

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OtpRoute(viewModel: OtpViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshCountdown() }
    OtpScreen(state, viewModel::input, viewModel::backspace, viewModel::focus, viewModel::submit, viewModel::resend)
}

@Composable
fun OtpScreen(
    state: OtpState,
    onInput: (Int, String) -> Int,
    onDelete: (Int) -> Int,
    onFocus: (Int) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
) {
    val colors = otpColors()
    val editors = remember { mutableMapOf<Int, DigitEditText>() }
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val inputRow = remember { BringIntoViewRequester() }
    LaunchedEffect(imeVisible, state.focus, state.focusRequest) {
        if (imeVisible) { delay(250); inputRow.bringIntoView() }
    }
    LaunchedEffect(state.verification) {
        if (state.verification == Verification.Success) keyboard?.hide()
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(colors["sheet"])) {
        Box(Modifier.fillMaxSize().background(colors["scrim"]))
        val topGap = if (imeVisible) 0.dp else (maxHeight * 0.12f).coerceIn(24.dp, 112.dp)
        Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 24.dp)
            .padding(top = topGap, bottom = if (imeVisible) 8.dp else 16.dp), contentAlignment = Alignment.BottomCenter) {
            Surface(modifier = Modifier.widthIn(max = 480.dp).fillMaxSize(),
                color = colors["sheet"], shape = RoundedCornerShape(36.dp)) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 16.dp, bottom = if (imeVisible) 12.dp else 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = if (imeVisible) 0.dp else 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(painterResource(R.drawable.thamani_logo), "Thamani logo", Modifier.size(if (imeVisible) 32.dp else 76.dp))
                        Spacer(Modifier.height(if (imeVisible) 8.dp else 16.dp))
                        Text("Enter your code", fontSize = 26.sp, fontWeight = FontWeight.Medium,
                            color = colors["text_primary"], textAlign = TextAlign.Center)
                        Spacer(Modifier.height(if (imeVisible) 4.dp else 12.dp))
                        Text("A 6-digit code has been sent to your phone.",
                            modifier = Modifier.widthIn(max = 260.dp), color = colors["text_primary"],
                            fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(if (imeVisible) 16.dp else 36.dp))
                        BoxWithConstraints(Modifier.fillMaxWidth().bringIntoViewRequester(inputRow)) {
                        val digitSize = (maxWidth / 6).coerceAtMost(48.dp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            repeat(OTP_LENGTH) { index ->
                                OtpDigitField(index, state.digits[index], state.focus == index, state.focusRequest, colors, digitSize, editors,
                                    { onInput(index, it) }, { onDelete(index) }, { onFocus(index) }, onSubmit)
                            }
                        }
                        }
                        Spacer(Modifier.height(if (imeVisible) 4.dp else 12.dp))
                        ResendControl(state.secondsRemaining, colors, onResend)
                        Spacer(Modifier.height(if (imeVisible) 0.dp else 24.dp))
                        }
                        Spacer(Modifier.height(if (imeVisible) 4.dp else 12.dp))
                        if (state.verification != Verification.Idle) Text(when (state.verification) {
                            Verification.Idle -> ""
                            Verification.Success -> "You're in."
                            Verification.Error -> "That code didn't work. Try again."
                        }, Modifier.fillMaxWidth().heightIn(min = if (state.verification == Verification.Idle) 0.dp else 36.dp).testTag("otp_status")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                            color = if (state.verification == Verification.Success) colors["success"] else colors["error"],
                            textAlign = TextAlign.Center, fontSize = 14.sp)
                        else Box(Modifier.testTag("otp_status"))
                        Button(onClick = onSubmit, enabled = state.complete,
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("otp_submit"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = colors["button_bg"],
                                contentColor = colors["button_text"], disabledContainerColor = colors["button_disabled_bg"],
                                disabledContentColor = colors["text_secondary"])) {
                            Text("Submit", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResendControl(seconds: Int, colors: OtpColors, onResend: () -> Unit) {
    // One wrapping label stays readable at large font scales and avoids duplicate announcements.
    TextButton(onClick = onResend, enabled = seconds == 0, modifier = Modifier.testTag("otp_resend"),
        colors = ButtonDefaults.textButtonColors(contentColor = colors["link"], disabledContentColor = colors["text_secondary"])) {
        Text(if (seconds > 0) "Resend in 0:${seconds.toString().padStart(2, '0')}"
            else "Didn't receive the code? Resend", textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Normal)
    }
}
