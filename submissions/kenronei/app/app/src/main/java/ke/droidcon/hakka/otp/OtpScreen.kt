package ke.droidcon.hakka.otp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
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
import ke.droidcon.hakka.AppInfo
import ke.droidcon.hakka.R
import kotlinx.coroutines.delay

/** Full OTP screen: a blurred, dimmed backdrop with the verification bottom sheet on top. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpScreen(colors: OtpColors = otpColors()) {
    var state by remember { mutableStateOf(OtpState()) }
    val update: ((OtpState) -> OtpState) -> Unit = { transform -> state = transform(state) }
    val keyboard = LocalSoftwareKeyboardController.current

    // 30 s resend countdown; restarts whenever OtpState.resend() bumps the generation.
    LaunchedEffect(state.countdownGeneration) {
        while (!state.canResend) {
            delay(1_000)
            update { it.tick() }
        }
    }

    val submit = {
        update { it.submit() }
        if (state.status == OtpState.Status.Success) keyboard?.hide()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        Backdrop(colors, Modifier.fillMaxSize().blur(16.dp))
        Box(Modifier.fillMaxSize().background(colors.scrim))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(12.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .background(colors.sheet, RoundedCornerShape(32.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.thamani_logo),
                contentDescription = "Thamani logo",
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Enter your code",
                color = colors.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "A 6-digit code has been sent to your phone.",
                color = colors.textSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            OtpInput(
                state = state,
                update = update,
                colors = colors,
                onDone = { if (state.canSubmit) submit() },
            )

            Spacer(Modifier.height(24.dp))
            ResendLine(
                state = state,
                colors = colors,
                onResend = { update { it.resend() } },
            )

            Spacer(Modifier.height(16.dp))
            val (statusText, statusColor) = when (state.status) {
                OtpState.Status.Success -> "You're in." to colors.success
                OtpState.Status.Error -> "That code didn't work. Try again." to colors.error
                OtpState.Status.Idle -> "" to Color.Unspecified
            }
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .heightIn(min = 20.dp)
                    .testTag("otp_status")
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = submit,
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
                Text("Submit", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ResendLine(state: OtpState, colors: OtpColors, onResend: () -> Unit) {
    val text = if (state.canResend) {
        buildAnnotatedString {
            append("Didn't receive the code? ")
            withStyle(SpanStyle(color = colors.link, fontWeight = FontWeight.SemiBold)) {
                append("Resend")
            }
        }
    } else {
        buildAnnotatedString { append("Resend in ${state.countdownLabel}") }
    }
    Text(
        text = text,
        color = colors.textSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .testTag("otp_resend")
            .clickable(enabled = state.canResend, role = Role.Button, onClick = onResend)
            .padding(8.dp),
    )
}

/** The screen "behind" the sheet; blurred (API 31+) and dimmed by the scrim. */
@Composable
private fun Backdrop(colors: OtpColors, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .background(colors.sheet)
            .statusBarsPadding()
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Welcome back", color = colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text(AppInfo.NAME, color = colors.textSecondary, fontSize = 16.sp)
        repeat(4) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(colors.digitEmptyBg, RoundedCornerShape(16.dp)),
            )
        }
    }
}

@Preview(name = "Light")
@Composable
private fun OtpScreenLightPreview() = OtpScreen(LightOtpColors)

@Preview(name = "Dark")
@Composable
private fun OtpScreenDarkPreview() = OtpScreen(DarkOtpColors)
