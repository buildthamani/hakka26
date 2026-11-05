package ke.droidcon.hakka

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(nowMillis: () -> Long = { SystemClock.elapsedRealtime() }) {
    var state by rememberSaveable { mutableStateOf(OtpState.initial(nowMillis())) }
    var seconds by remember { mutableIntStateOf(state.secondsRemaining(nowMillis())) }
    val colors = LocalOtpColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(state.deadlineMillis) {
        do {
            seconds = state.secondsRemaining(nowMillis())
            if (seconds > 0) delay(100)
        } while (seconds > 0)
    }
    fun submit() {
        state = state.submit()
        if (state.successful) keyboard?.hide()
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text("Welcome back", color = colors.secondary,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 44.dp))
        Box(Modifier.fillMaxSize().background(colors.scrim))
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.BottomCenter) {
            val compact = maxHeight < 600.dp
            val sheetHeight = if (compact) maxHeight else maxHeight * 0.88f
            Surface(Modifier.widthIn(max = 440.dp).fillMaxWidth().height(sheetHeight)
                .testTag("otp_sheet"), color = colors.sheet, shape = RoundedCornerShape(32.dp)) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = if (compact) 16.dp else 24.dp)) {
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(if (compact) 0.dp else 16.dp))
                        Image(painterResource(R.drawable.thamani_logo), "Thamani logo", Modifier.size(if (compact) 40.dp else 64.dp))
                        Spacer(Modifier.height(if (compact) 8.dp else 20.dp))
                        Text("Enter your code", style = MaterialTheme.typography.headlineSmall, color = colors.text)
                        Spacer(Modifier.height(8.dp))
                        Text("A 6-digit code has been sent to your phone.", color = colors.text,
                            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 260.dp))
                        Spacer(Modifier.height(if (compact) 16.dp else 36.dp))
                        OtpInput(state, colors, onChange = { state = it(state) }, onSubmit = { submit() })
                        Spacer(Modifier.height(16.dp))
                        ResendControl(seconds, colors) {
                            state = state.resend(nowMillis())
                            seconds = state.secondsRemaining(nowMillis())
                        }
                        Text(state.status, color = if (state.successful) colors.success else colors.error,
                            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp).testTag("otp_status").semantics {
                                liveRegion = LiveRegionMode.Polite
                            })
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { submit() }, enabled = state.complete,
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.button,
                            contentColor = colors.buttonText, disabledContainerColor = colors.disabled,
                            disabledContentColor = colors.secondary),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("otp_submit")) {
                        Text("Submit")
                    }
                    Spacer(Modifier.height(if (compact) 8.dp else 20.dp))
                }
            }
        }
    }
}

@Composable
private fun ResendControl(seconds: Int, colors: OtpColors, onResend: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text("Didn't receive the code?", style = MaterialTheme.typography.bodySmall, color = colors.secondary)
        TextButton(onClick = onResend, enabled = seconds == 0,
            modifier = Modifier.testTag("otp_resend"),
            contentPadding = PaddingValues(horizontal = 4.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = colors.link,
                disabledContentColor = colors.secondary)) {
            Text(if (seconds > 0) "Resend in 0:%02d".format(seconds) else "Resend",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
