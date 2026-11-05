package ke.droidcon.hakka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HakkaTheme {
                OtpScreen()
            }
        }
    }
}

/** Owns the [OtpState]; every event derives the next state from the latest one. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpScreen() {
    var state by remember { mutableStateOf(OtpState()) }
    val keyboard = LocalSoftwareKeyboardController.current

    // 30 s resend countdown; restarts whenever resendCount changes.
    LaunchedEffect(state.resendCount) {
        while (state.resendSecondsLeft > 0) {
            delay(1_000)
            state = state.tick()
        }
    }

    val submit = {
        state = state.submit()
        if (state.status == OtpStatus.Success) keyboard?.hide()
    }

    Box(
        Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        Backdrop(Modifier.fillMaxSize().blur(16.dp))
        Box(
            Modifier
                .fillMaxSize()
                .background(LocalOtpColors.current.scrim),
        )
        OtpSheet(
            state = state,
            onInput = { i, raw -> state = state.input(i, raw) },
            onBackspace = { i -> state = state.backspace(i) },
            onFocused = { i -> state = state.onFocused(i) },
            onSubmit = submit,
            onResend = { state = state.resend() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .statusBarsPadding()
                .padding(top = 48.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
        )
    }
}

/** App content shown dimmed and blurred behind the sheet. */
@Composable
private fun Backdrop(modifier: Modifier = Modifier) {
    val colors = LocalOtpColors.current
    Column(
        modifier
            .background(colors.sheet)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.thamani_logo),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
        )
        Text(AppInfo.NAME, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        repeat(4) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(colors.digitEmptyBg, RoundedCornerShape(16.dp)),
            )
        }
    }
}

@Composable
fun OtpSheet(
    state: OtpState,
    onInput: (Int, String) -> Unit,
    onBackspace: (Int) -> Unit,
    onFocused: (Int) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalOtpColors.current
    Column(
        modifier
            .background(colors.sheet, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .width(40.dp)
                .height(4.dp)
                .background(colors.digitEmptyOutline, RoundedCornerShape(2.dp)),
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            Image(
                painter = painterResource(R.drawable.thamani_logo),
                contentDescription = "Thamani logo",
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                OtpCopy.TITLE,
                color = colors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                OtpCopy.SUBTITLE,
                color = colors.textSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            OtpInput(
                state = state,
                onInput = onInput,
                onBackspace = onBackspace,
                onFocused = onFocused,
                onDone = { if (state.canSubmit) onSubmit() },
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = state.statusMessage,
                color = if (state.status == OtpStatus.Success) colors.success else colors.error,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("otp_status")
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
            Spacer(Modifier.height(12.dp))
            ResendLine(state, onResend)
            Spacer(Modifier.height(24.dp))
        }
        Button(
            onClick = onSubmit,
            enabled = state.canSubmit,
            shape = RoundedCornerShape(percent = 50),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.buttonBg,
                contentColor = colors.buttonText,
                disabledContainerColor = colors.buttonDisabledBg,
                disabledContentColor = colors.buttonText,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("otp_submit"),
        ) {
            Text(OtpCopy.SUBMIT, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ResendLine(state: OtpState, onResend: () -> Unit) {
    val colors = LocalOtpColors.current
    val text = if (state.canResend) {
        buildAnnotatedString {
            append(OtpCopy.RESEND_PROMPT)
            withStyle(SpanStyle(color = colors.link, fontWeight = FontWeight.Bold)) { append(OtpCopy.RESEND) }
        }
    } else {
        buildAnnotatedString { append(OtpCopy.resendCountdown(state.resendSecondsLeft)) }
    }
    Text(
        text = text,
        color = colors.textSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .testTag("otp_resend")
            .clickable(
                enabled = state.canResend,
                role = Role.Button,
                onClickLabel = OtpCopy.RESEND,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onResend,
            )
            .padding(8.dp),
    )
}

@Preview
@Composable
private fun OtpScreenPreview() {
    HakkaTheme(darkTheme = false) { OtpScreen() }
}

@Preview
@Composable
private fun OtpScreenDarkPreview() {
    HakkaTheme(darkTheme = true) { OtpScreen() }
}
