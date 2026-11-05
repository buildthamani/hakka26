package ke.droidcon.hakka.otp

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ke.droidcon.hakka.HakkaTheme
import ke.droidcon.hakka.R
import kotlinx.coroutines.flow.first

@Composable
fun OtpScreen(viewModel: OtpViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    OtpScreen(
        state = uiState,
        onCodeChange = viewModel::onCodeChange,
        onSubmit = viewModel::onSubmit,
        onResend = viewModel::onResend,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpScreen(
    state: OtpUiState,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
) {
    val colors = LocalOtpColors.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val windowInfo = LocalWindowInfo.current

    // Focus the code field (and so the first box) as soon as the window is ready, which opens the numeric
    // keyboard, and again after a wrong code has cleared the boxes.
    LaunchedEffect(state.status) {
        if (state.status == OtpStatus.Success) {
            keyboard?.hide()
        } else {
            snapshotFlow { windowInfo.isWindowFocused }.first { it }
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        PageBehindSheet()
        Box(Modifier.fillMaxSize().background(colors.scrim))
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .background(colors.sheet, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 16.dp),
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
                    contentDescription = "Thamani logo",
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Enter your code",
                    color = colors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "A 6-digit code has been sent to your phone.",
                    color = colors.textSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                OtpCodeInput(
                    code = state.code,
                    focusedIndex = state.focusedIndex,
                    onCodeChange = onCodeChange,
                    onDone = onSubmit,
                    focusRequester = focusRequester,
                )
                Spacer(Modifier.height(16.dp))
                StatusMessage(state)
                Spacer(Modifier.height(4.dp))
                ResendLine(state, onResend)
            }
            Spacer(Modifier.height(16.dp))
            SubmitButton(enabled = state.isComplete, onClick = onSubmit)
        }
    }
}

/** The page the sheet is shown over: dimmed by the scrim and, on Android 12+, blurred. */
@Composable
private fun PageBehindSheet() {
    val colors = LocalOtpColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.sheet)
            .blur(12.dp)
            .statusBarsPadding()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Welcome back",
            color = colors.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatusMessage(state: OtpUiState) {
    val colors = LocalOtpColors.current
    Text(
        text = state.statusMessage,
        color = if (state.status == OtpStatus.Error) colors.error else colors.success,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .heightIn(min = 22.dp)
            .testTag("otp_status")
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun ResendLine(state: OtpUiState, onResend: () -> Unit) {
    val colors = LocalOtpColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Didn't receive the code? ",
            color = colors.textSecondary,
            fontSize = 14.sp,
        )
        Text(
            text = state.resendLabel,
            color = if (state.canResend) colors.link else colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .testTag("otp_resend")
                .minimumInteractiveComponentSize()
                .clickable(enabled = state.canResend, role = Role.Button, onClick = onResend),
        )
    }
}

@Composable
private fun SubmitButton(enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalOtpColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
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
        Text(text = "Submit", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OtpScreenPreview() {
    HakkaTheme {
        OtpScreen(state = OtpUiState(code = "215"), onCodeChange = {}, onSubmit = {}, onResend = {})
    }
}
