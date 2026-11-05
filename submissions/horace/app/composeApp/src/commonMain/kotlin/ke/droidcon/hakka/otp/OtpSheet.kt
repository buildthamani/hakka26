package ke.droidcon.hakka.otp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hakka.composeapp.generated.resources.Res
import hakka.composeapp.generated.resources.thamani_logo
import ke.droidcon.hakka.theme.OtpColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/** The OTP bottom sheet over a dimmed, blurred backdrop. */
@Composable
fun OtpScreen(state: OtpState, colors: OtpColors) {
    // Resend countdown: restarts each time the user resends.
    LaunchedEffect(state.resendCount) {
        while (state.resendSecondsRemaining > 0) {
            delay(1_000)
            state.tick()
        }
    }

    Box(Modifier.fillMaxSize().background(colors.sheet)) {
        BackdropContent(colors)
        Box(Modifier.fillMaxSize().background(colors.scrim))
        Box(
            Modifier.fillMaxSize().imePadding().statusBarsPadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            OtpSheet(state, colors, Modifier.fillMaxWidth().fillMaxHeight(0.88f))
        }
    }
}

@Composable
private fun OtpSheet(state: OtpState, colors: OtpColors, modifier: Modifier = Modifier) {
    val succeeded = state.status == OtpStatus.Success
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(colors.sheet, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.thamani_logo),
            contentDescription = "Thamani logo",
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            OtpCopy.TITLE,
            color = colors.textPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            OtpCopy.SUBTITLE,
            color = colors.textSecondary,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        OtpInput(state, colors, enabled = !succeeded)
        Spacer(Modifier.height(28.dp))
        ResendLine(state, colors)
        Spacer(Modifier.height(20.dp))
        StatusMessage(state.status, colors)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = state::submit,
            enabled = state.isComplete && !succeeded,
            shape = RoundedCornerShape(percent = 50),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.buttonBg,
                contentColor = colors.buttonText,
                disabledContainerColor = colors.buttonDisabledBg,
                disabledContentColor = colors.buttonText,
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("otp_submit"),
        ) {
            Text(OtpCopy.SUBMIT, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ResendLine(state: OtpState, colors: OtpColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(OtpCopy.RESEND_PROMPT, color = colors.textSecondary, fontSize = 14.sp)
        Text(
            text = if (state.canResend) OtpCopy.RESEND else OtpCopy.resendIn(state.resendSecondsRemaining),
            color = if (state.canResend) colors.link else colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = if (state.canResend) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier
                .testTag("otp_resend")
                .clickable(enabled = state.canResend, role = Role.Button, onClick = state::resend),
        )
    }
}

@Composable
private fun StatusMessage(status: OtpStatus, colors: OtpColors) {
    val (text, color) = when (status) {
        OtpStatus.Success -> OtpCopy.SUCCESS to colors.success
        OtpStatus.Error -> OtpCopy.ERROR to colors.error
        OtpStatus.Idle -> "" to colors.textSecondary
    }
    Text(
        text,
        color = color,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .testTag("otp_status")
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Stand-in for the app screen behind the sheet, so the scrim has something to dim. */
@Composable
private fun BackdropContent(colors: OtpColors) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize().blur(8.dp).statusBarsPadding().padding(24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Welcome back", color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        repeat(4) {
            Box(
                Modifier.fillMaxWidth().height(56.dp)
                    .background(colors.digitEmptyBg, RoundedCornerShape(16.dp)),
            )
        }
    }
}
