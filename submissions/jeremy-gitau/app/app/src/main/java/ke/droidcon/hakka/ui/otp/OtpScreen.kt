package ke.droidcon.hakka.ui.otp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.droidcon.hakka.R
import ke.droidcon.hakka.domain.FixedOtpVerifier
import ke.droidcon.hakka.domain.OtpVerifier
import ke.droidcon.hakka.ui.otp.components.OtpInput
import ke.droidcon.hakka.ui.theme.HakkaTheme
import kotlinx.coroutines.delay

/** Stateful entry point: owns [OtpState] and feeds every event through [OtpReducer]. */
@Composable
fun OtpScreen(verifier: OtpVerifier = FixedOtpVerifier()) {
    val reducer = remember(verifier) { OtpReducer(verifier) }
    var state by remember { mutableStateOf(OtpState()) }
    val dispatch: (OtpAction) -> Unit = { state = reducer.reduce(state, it) }

    // Resend countdown: one tick per second until it reaches 0.
    LaunchedEffect(state.resendSecondsLeft) {
        if (state.resendSecondsLeft > 0) {
            delay(1_000)
            dispatch(OtpAction.Tick)
        }
    }

    OtpScreenContent(state = state, onAction = dispatch)
}

/** Stateless UI: a bottom sheet over a dimmed, blurred background. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpScreenContent(state: OtpState, onAction: (OtpAction) -> Unit) {
    val colors = HakkaTheme.otpColors
    Box(
        Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        BackgroundScreen(Modifier.blur(8.dp))
        Box(Modifier.fillMaxSize().background(colors.scrim))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .background(colors.sheet, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Image(
                painter = painterResource(
                    if (isSystemInDarkTheme()) R.drawable.thamani_logo_white else R.drawable.thamani_logo,
                ),
                contentDescription = stringResource(R.string.otp_logo),
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.otp_title),
                color = colors.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.otp_subtitle),
                color = colors.textSecondary,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(28.dp))
            OtpInput(
                code = state.code,
                length = OTP_LENGTH,
                onCodeChange = { onAction(OtpAction.Input(it)) },
            )
            Spacer(Modifier.height(28.dp))
            ResendLine(state = state, onResend = { onAction(OtpAction.Resend) })
            Spacer(Modifier.height(16.dp))
            StatusMessage(state.status)
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onAction(OtpAction.Submit) },
                enabled = state.canSubmit,
                shape = RoundedCornerShape(28.dp),
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
                Text(stringResource(R.string.otp_submit), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ResendLine(state: OtpState, onResend: () -> Unit) {
    val colors = HakkaTheme.otpColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (state.canResend) {
            Text(stringResource(R.string.otp_resend_prompt), color = colors.textSecondary, fontSize = 14.sp)
        }
        Text(
            text = if (state.canResend) {
                stringResource(R.string.otp_resend)
            } else {
                stringResource(R.string.otp_resend_countdown, formatCountdown(state.resendSecondsLeft))
            },
            color = if (state.canResend) colors.link else colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = if (state.canResend) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier
                .testTag("otp_resend")
                .clickable(enabled = state.canResend, role = Role.Button, onClick = onResend),
        )
    }
}

@Composable
private fun StatusMessage(status: OtpStatus) {
    val colors = HakkaTheme.otpColors
    val (text, color) = when (status) {
        OtpStatus.Success -> stringResource(R.string.otp_success) to colors.success
        OtpStatus.Error -> stringResource(R.string.otp_error) to colors.error
        OtpStatus.Idle -> "" to colors.textSecondary
    }
    Text(
        text = text,
        color = color,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .heightIn(min = 20.dp)
            .testTag("otp_status")
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Placeholder app screen behind the sheet, dimmed by the scrim. */
@Composable
private fun BackgroundScreen(modifier: Modifier = Modifier) {
    val colors = HakkaTheme.otpColors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = modifier
            .fillMaxSize()
            .background(colors.sheet)
            .statusBarsPadding()
            .padding(top = 48.dp),
    ) {
        Text(stringResource(R.string.otp_background_title), color = colors.textPrimary, fontSize = 22.sp)
    }
}

@Preview(showSystemUi = true)
@Composable
private fun OtpScreenPreview() {
    HakkaTheme { OtpScreenContent(state = OtpState(code = "21"), onAction = {}) }
}
