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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.droidcon.hakka.R
import kotlinx.coroutines.delay

/** Wires [OtpStateHolder] to the UI and drives the resend countdown. */
@Composable
fun OtpScreen(modifier: Modifier = Modifier) {
    var uiState by remember { mutableStateOf(OtpUiState()) }
    val holder = remember { OtpStateHolder(onStateChanged = { uiState = it }) }

    LaunchedEffect(uiState.isCountingDown) {
        while (holder.state.isCountingDown) {
            delay(1_000)
            holder.onTick()
        }
    }

    OtpSheetScaffold(modifier) {
        OtpSheetContent(
            state = uiState,
            onInput = holder::onInput,
            onBackspace = holder::onBackspace,
            onFocus = holder::onFocus,
            onSubmit = holder::onSubmit,
            onResend = holder::onResend,
        )
    }
}

/** A dimmed (and, on API 31+, blurred) backdrop with the sheet anchored to the bottom. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun OtpSheetScaffold(modifier: Modifier = Modifier, sheet: @Composable () -> Unit) {
    val colors = LocalOtpColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.sheet)
            .semantics { testTagsAsResourceId = true },
    ) {
        BackdropScreen(Modifier.blur(6.dp))
        Box(Modifier.fillMaxSize().background(colors.scrim))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = colors.sheet,
            ) { sheet() }
        }
    }
}

/** Decorative screen behind the sheet; hidden from screen readers. */
@Composable
private fun BackdropScreen(modifier: Modifier = Modifier) {
    val colors = LocalOtpColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
            .clearAndSetSemantics {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.backdrop_title), color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        repeat(3) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(colors.digitEmptyBg, RoundedCornerShape(16.dp)),
            )
        }
    }
}

@Composable
private fun OtpSheetContent(
    state: OtpUiState,
    onInput: (Int, String) -> Unit,
    onBackspace: (Int) -> Unit,
    onFocus: (Int) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
) {
    val colors = LocalOtpColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.thamani_logo),
                contentDescription = stringResource(R.string.otp_logo_description),
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.otp_title),
                color = colors.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.otp_subtitle),
                color = colors.textSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            OtpInput(
                state = state,
                onInput = onInput,
                onBackspace = onBackspace,
                onFocus = onFocus,
                enabled = state.status != OtpStatus.Success,
            )
            Spacer(Modifier.height(24.dp))
            ResendLine(
                secondsLeft = state.resendSecondsLeft,
                canResend = state.canResend,
                onResend = onResend,
            )
            Spacer(Modifier.height(12.dp))
            StatusMessage(state.status)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            enabled = state.canSubmit && state.status != OtpStatus.Success,
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
            Text(stringResource(R.string.otp_submit), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ResendLine(secondsLeft: Int, canResend: Boolean, onResend: () -> Unit) {
    val colors = LocalOtpColors.current
    val text = if (canResend) {
        buildAnnotatedString {
            append(stringResource(R.string.otp_resend_prompt))
            append(' ')
            withStyle(SpanStyle(color = colors.link, fontWeight = FontWeight.SemiBold)) {
                append(stringResource(R.string.otp_resend_action))
            }
        }
    } else {
        buildAnnotatedString {
            append(stringResource(R.string.otp_resend_countdown, formatCountdown(secondsLeft)))
        }
    }
    Text(
        text = text,
        color = colors.textSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .testTag("otp_resend")
            .clickable(enabled = canResend, role = Role.Button, onClick = onResend)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun StatusMessage(status: OtpStatus) {
    val colors = LocalOtpColors.current
    val (message, color) = when (status) {
        OtpStatus.Success -> stringResource(R.string.otp_success) to colors.success
        OtpStatus.Error -> stringResource(R.string.otp_error) to colors.error
        OtpStatus.Idle -> "" to Color.Unspecified
    }
    Text(
        text = message,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .heightIn(min = 20.dp)
            .testTag("otp_status")
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
