package ke.droidcon.hakka.otp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ke.droidcon.hakka.R
import ke.droidcon.hakka.ui.theme.HakkaTheme
import ke.droidcon.hakka.ui.theme.LocalOtpColors

@Composable
fun OtpRoute(viewModel: OtpViewModel = viewModel { OtpViewModel() }) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OtpScreen(
        state = state,
        onBoxTextChange = viewModel::onBoxTextChange,
        onBackspaceOnEmptyBox = viewModel::onBackspaceOnEmptyBox,
        onBoxFocused = viewModel::onBoxFocused,
        onSubmit = viewModel::onSubmit,
        onResend = viewModel::onResend,
    )
}

/** The OTP bottom sheet shown over a blurred, dimmed backdrop. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OtpScreen(
    state: OtpUiState,
    onBoxTextChange: (Int, String) -> Int,
    onBackspaceOnEmptyBox: (Int) -> Int,
    onBoxFocused: (Int) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
) {
    val colors = LocalOtpColors.current
    val sheetHeightFraction = if (WindowInsets.isImeVisible) 0.94f else 0.88f
    BoxWithConstraints(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
        BlurredBackdrop(Modifier.fillMaxSize().blur(16.dp))
        Box(Modifier.fillMaxSize().background(colors.scrim))
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(maxHeight * sheetHeightFraction),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = colors.sheet,
        ) {
            OtpSheetContent(
                state = state,
                onBoxTextChange = onBoxTextChange,
                onBackspaceOnEmptyBox = onBackspaceOnEmptyBox,
                onBoxFocused = onBoxFocused,
                onSubmit = onSubmit,
                onResend = onResend,
            )
        }
    }
}

@Composable
private fun OtpSheetContent(
    state: OtpUiState,
    onBoxTextChange: (Int, String) -> Int,
    onBackspaceOnEmptyBox: (Int) -> Int,
    onBoxFocused: (Int) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
) {
    val colors = LocalOtpColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
            Spacer(Modifier.height(16.dp))
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
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            OtpInput(
                digits = state.digits,
                focusedIndex = state.focusedIndex,
                onBoxTextChange = onBoxTextChange,
                onBackspaceOnEmptyBox = onBackspaceOnEmptyBox,
                onBoxFocused = onBoxFocused,
                onDone = onSubmit,
            )
            Spacer(Modifier.height(16.dp))
            ResendLine(secondsLeft = state.resendSecondsLeft, onResend = onResend)
            StatusMessage(status = state.status, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            enabled = state.isComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("otp_submit"),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.buttonBg,
                contentColor = colors.buttonText,
                disabledContainerColor = colors.buttonDisabledBg,
                disabledContentColor = colors.buttonText.copy(alpha = 0.7f),
            ),
        ) {
            Text(stringResource(R.string.otp_submit), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ResendLine(secondsLeft: Int, onResend: () -> Unit) {
    val colors = LocalOtpColors.current
    val canResend = secondsLeft == 0
    val text = if (canResend) {
        buildAnnotatedString {
            append(stringResource(R.string.otp_resend_prompt))
            append(" ")
            withStyle(SpanStyle(color = colors.link, fontWeight = FontWeight.SemiBold)) {
                append(stringResource(R.string.otp_resend_action))
            }
        }
    } else {
        AnnotatedString(stringResource(R.string.otp_resend_countdown, formatCountdown(secondsLeft)))
    }
    Text(
        text = text,
        color = colors.textSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = canResend, role = Role.Button, onClick = onResend)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("otp_resend"),
    )
}

@Composable
private fun StatusMessage(status: OtpStatus, modifier: Modifier = Modifier) {
    val colors = LocalOtpColors.current
    val (message, color) = when (status) {
        OtpStatus.None -> return
        OtpStatus.Success -> stringResource(R.string.otp_success) to colors.success
        OtpStatus.Error -> stringResource(R.string.otp_error) to colors.error
    }
    Text(
        text = message,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("otp_status"),
    )
}

/** Stand-in for the screen behind the sheet, so the blur and dim have something to show. */
@Composable
private fun BlurredBackdrop(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.background_welcome),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        repeat(4) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun OtpScreenPreview() {
    HakkaTheme {
        OtpScreen(
            state = OtpUiState(digits = listOf("2", "1", "5", "", "", ""), focusedIndex = 2),
            onBoxTextChange = { _, _ -> 0 },
            onBackspaceOnEmptyBox = { 0 },
            onBoxFocused = {},
            onSubmit = {},
            onResend = {},
        )
    }
}
