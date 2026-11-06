package ke.droidcon.hakka

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val OtpStateSaver = listSaver<OtpState, String>(
    save = { it.digits + listOf(it.focus.toString(), it.status) },
    restore = { OtpState(it.take(6), it[6].toInt(), it[7]) },
)

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun OtpScreen(dark: Boolean = isSystemInDarkTheme()) {
    val palette = if (dark) DarkOtp else LightOtp
    var state by rememberSaveable(stateSaver = OtpStateSaver) { mutableStateOf(OtpState()) }
    var deadline by rememberSaveable { mutableLongStateOf(SystemClock.elapsedRealtime() + 30_000) }
    val remaining = rememberCountdown(deadline)
    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color(0xFFE6E9E7))
            .semantics { testTagsAsResourceId = true },
    ) {
        Box(Modifier.fillMaxSize().background(palette.scrim))
        Surface(
            color = palette.sheet,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(horizontal = maxWidth * .06f)
                .padding(bottom = maxHeight * .038f)
                .height(maxHeight * .82f).fillMaxWidth().imePadding(),
        ) {
            OtpSheet(
                state = state, palette = palette, remaining = remaining,
                onChange = { state = it },
                onResend = { deadline = SystemClock.elapsedRealtime() + 30_000 },
            )
        }
    }
}

@Composable
private fun rememberCountdown(deadline: Long): Int {
    var remaining by remember(deadline) {
        mutableIntStateOf(remainingSeconds(deadline, SystemClock.elapsedRealtime()))
    }
    LaunchedEffect(deadline) {
        do {
            remaining = remainingSeconds(deadline, SystemClock.elapsedRealtime())
            if (remaining > 0) delay(200)
        } while (remaining > 0)
    }
    return remaining
}

@Composable
private fun OtpSheet(
    state: OtpState,
    palette: OtpPalette,
    remaining: Int,
    onChange: (OtpState) -> Unit,
    onResend: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        Column(
            Modifier.verticalScroll(rememberScrollState()).heightIn(min = availableHeight)
                .navigationBarsPadding().padding(horizontal = 20.dp)
                .padding(top = 32.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OtpHeading(palette)
            Spacer(Modifier.height(36.dp))
            OtpInput(state, palette, onChange)
            Spacer(Modifier.height(18.dp))
            ResendCode(remaining, palette, onResend)
            Text(
                state.status,
                color = if (state.status == "You're in.") palette.success else palette.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("otp_status").semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.weight(1f).heightIn(min = 36.dp))
            Button(
                onClick = { onChange(state.submit()) },
                enabled = state.complete, shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("otp_submit"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.filled, contentColor = Color.White,
                    disabledContainerColor = palette.disabled, disabledContentColor = Color.White,
                ),
            ) { Text("Submit") }
        }
    }
}

@Composable
private fun OtpHeading(palette: OtpPalette) {
    Image(
        painterResource(R.drawable.otp_flower), contentDescription = "Flower logo",
        modifier = Modifier.size(64.dp),
    )
    Spacer(Modifier.height(18.dp))
    Text("Enter your code", color = palette.primary, fontSize = 26.sp)
    Spacer(Modifier.height(10.dp))
    Text(
        "A 6-digit code has been sent to your phone.", color = palette.secondary,
        fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 260.dp),
    )
}

@Composable
private fun ResendCode(remaining: Int, palette: OtpPalette, onResend: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Didn't receive the code?", color = palette.secondary, fontSize = 12.sp)
        TextButton(
            enabled = remaining == 0, onClick = onResend,
            modifier = Modifier.testTag("otp_resend"),
            colors = ButtonDefaults.textButtonColors(
                contentColor = palette.link, disabledContentColor = palette.secondary,
            ),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            Text(
                if (remaining > 0) "Resend in 0:${remaining.toString().padStart(2, '0')}" else "Resend",
                fontSize = 12.sp,
            )
        }
    }
}
