package ke.droidcon.hakka

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { OtpScreen() } }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun OtpScreen(dark: Boolean = isSystemInDarkTheme()) {
    val palette = if (dark) DarkOtp else LightOtp
    var digits by rememberSaveable { mutableStateOf(List(6) { "" }) }
    var status by rememberSaveable { mutableStateOf("") }
    var focus by rememberSaveable { mutableIntStateOf(0) }
    var focusRequest by remember { mutableIntStateOf(0) }
    var deadline by rememberSaveable { mutableLongStateOf(SystemClock.elapsedRealtime() + 30_000) }
    var remaining by remember { mutableIntStateOf(remainingSeconds(deadline, SystemClock.elapsedRealtime())) }
    fun apply(next: OtpState) {
        digits = next.digits
        status = next.status
        focus = next.focus
        focusRequest++
    }
    LaunchedEffect(deadline) {
        do {
            remaining = remainingSeconds(deadline, SystemClock.elapsedRealtime())
            if (remaining > 0) delay(200)
        } while (remaining > 0)
    }
    Box(Modifier.fillMaxSize().background(Color(0xFFE6E9E7)).semantics { testTagsAsResourceId = true }) {
        Box(Modifier.fillMaxSize().background(palette.scrim))
        Surface(
            color = palette.sheet,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.86f)
                .imePadding(),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
            val availableHeight = maxHeight
            Column(
                Modifier.verticalScroll(rememberScrollState()).heightIn(min = availableHeight).navigationBarsPadding().padding(horizontal = 20.dp).padding(top = 32.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painterResource(if (dark) R.drawable.thamani_logo_white else R.drawable.thamani_logo),
                    contentDescription = "Thamani logo", modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(18.dp))
                Text("Enter your code", color = palette.primary, fontSize = 26.sp)
                Spacer(Modifier.height(10.dp))
                Text("A 6-digit code has been sent to your phone.", color = palette.secondary,
                    fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 260.dp))
                Spacer(Modifier.height(36.dp))
                OtpInput(
                    digits = digits,
                    focus = focus,
                    status = status,
                    focusRequest = focusRequest,
                    palette = palette,
                    onChange = ::apply,
                )
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Didn't receive the code?", color = palette.secondary, fontSize = 12.sp)
                    TextButton(
                        enabled = remaining == 0,
                        onClick = { deadline = SystemClock.elapsedRealtime() + 30_000; remaining = 30 },
                        modifier = Modifier.testTag("otp_resend"),
                        colors = ButtonDefaults.textButtonColors(contentColor = palette.link, disabledContentColor = palette.secondary),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) { Text(if (remaining > 0) "Resend in 0:${remaining.toString().padStart(2, '0')}" else "Resend", fontSize = 12.sp) }
                }
                Text(status, color = if (status == "You're in.") palette.success else palette.error,
                    textAlign = TextAlign.Center, modifier = Modifier.testTag("otp_status").semantics { liveRegion = LiveRegionMode.Polite })
                Spacer(Modifier.weight(1f).heightIn(min = 36.dp))
                Button(
                    onClick = { apply(OtpState(digits, focus, status).submit()) },
                    enabled = digits.all { it.length == 1 },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("otp_submit"),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.filled, contentColor = Color.White,
                        disabledContainerColor = palette.disabled, disabledContentColor = Color.White),
                ) { Text("Submit") }
            }
            }
        }
    }
}
