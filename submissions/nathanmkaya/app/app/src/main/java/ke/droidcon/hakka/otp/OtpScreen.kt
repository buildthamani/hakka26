package ke.droidcon.hakka.otp

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.droidcon.hakka.HakkaTheme
import ke.droidcon.hakka.R
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(modifier: Modifier = Modifier) {
    var state by rememberSaveable(stateSaver = OtpStateSaver) { mutableStateOf(OtpState()) }
    LaunchedEffect(state.resendSecondsLeft) {
        if (state.resendSecondsLeft > 0) {
            delay(1_000)
            state = reduce(state, OtpAction.Tick)
        }
    }
    OtpSheet(state = state, onAction = { state = reduce(state, it) }, modifier = modifier)
}

@Composable
fun OtpSheet(state: OtpState, onAction: (OtpAction) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalOtpColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.sheet)
            .semantics { testTagsAsResourceId = true },
    ) {
        SignInBackdrop(colors)
        Box(Modifier.fillMaxSize().background(colors.scrim))
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(32.dp))
                .background(colors.sheet)
                .padding(horizontal = 20.dp, vertical = 28.dp),
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
                    modifier = Modifier.height(56.dp),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.otp_title),
                    color = colors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.otp_subtitle),
                    color = colors.textSecondary,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 250.dp),
                )
                Spacer(Modifier.height(32.dp))
                OtpInput(
                    digits = state.digits,
                    focusedIndex = state.focusedIndex,
                    onInput = { index, text -> onAction(OtpAction.Input(index, text)) },
                    onBackspace = { onAction(OtpAction.Backspace(it)) },
                    onFocus = { onAction(OtpAction.Focus(it)) },
                    modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
                )
                Spacer(Modifier.height(12.dp))
                ResendLine(
                    secondsLeft = state.resendSecondsLeft,
                    canResend = state.canResend,
                    onResend = { onAction(OtpAction.Resend) },
                    colors = colors,
                )
                StatusMessage(status = state.status, colors = colors)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onAction(OtpAction.Submit) },
                enabled = state.isComplete,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.buttonBg,
                    contentColor = colors.buttonText,
                    disabledContainerColor = colors.buttonDisabledBg,
                    disabledContentColor = colors.buttonText,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("otp_submit"),
            ) {
                Text(stringResource(R.string.otp_submit), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ResendLine(secondsLeft: Int, canResend: Boolean, onResend: () -> Unit, colors: OtpColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.otp_resend_prompt), color = colors.textSecondary, fontSize = 14.sp)
        Text(
            text = if (canResend) {
                stringResource(R.string.otp_resend)
            } else {
                stringResource(R.string.otp_resend_countdown, secondsLeft / 60, secondsLeft % 60)
            },
            color = if (canResend) colors.link else colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = if (canResend) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier
                .testTag("otp_resend")
                .clickable(enabled = canResend, role = Role.Button, onClick = onResend)
                .padding(horizontal = 4.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun StatusMessage(status: OtpStatus, colors: OtpColors) {
    val (text, color) = when (status) {
        OtpStatus.Idle -> "" to colors.textSecondary
        OtpStatus.Success -> stringResource(R.string.otp_success) to colors.success
        OtpStatus.Error -> stringResource(R.string.otp_error) to colors.error
    }
    Text(
        text = text,
        color = color,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .testTag("otp_status")
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** The screen the sheet was raised over, blurred and hidden from screen readers. */
@Composable
private fun SignInBackdrop(colors: OtpColors) {
    Text(
        text = stringResource(R.string.backdrop_title),
        color = colors.textPrimary,
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 48.dp)
            .blur(6.dp)
            .clearAndSetSemantics {},
    )
}

private val OtpStateSaver = listSaver<OtpState, Any>(
    save = { state ->
        listOf(
            state.digits.joinToString("") { it?.toString() ?: " " },
            state.focusedIndex,
            state.status.name,
            state.resendSecondsLeft,
        )
    },
    restore = { saved ->
        OtpState(
            digits = (saved[0] as String).map { it.takeUnless(Char::isWhitespace) },
            focusedIndex = saved[1] as Int,
            status = OtpStatus.valueOf(saved[2] as String),
            resendSecondsLeft = saved[3] as Int,
        )
    },
)

private val partiallyFilled = OtpState(digits = listOf('2', '1', null, null, null, null), focusedIndex = 2)

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OtpSheetPreview() {
    HakkaTheme { OtpSheet(state = partiallyFilled, onAction = {}) }
}
