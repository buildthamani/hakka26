package ke.droidcon.hakka.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.droidcon.hakka.R
import ke.droidcon.hakka.domain.model.OtpState
import ke.droidcon.hakka.domain.model.OtpStatus
import ke.droidcon.hakka.presentation.OtpColors

@Composable
fun OtpBottomSheetContent(
    state: OtpState,
    colors: OtpColors,
    onDigitInput: (index: Int, text: String) -> Unit,
    onPaste: (String) -> Unit,
    onBackspace: (index: Int) -> Unit,
    onFocused: (index: Int) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.86f)
            .background(
                colors.sheet,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            )
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.thamani_logo),
            contentDescription = stringResource(R.string.thamani_logo_description),
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.enter_your_code),
            color = colors.textPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.code_sent),
            color = colors.textSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        OtpInputRow(
            state = state,
            colors = colors,
            onInput = onDigitInput,
            onPaste = onPaste,
            onBackspace = onBackspace,
            onFocused = onFocused,
        )
        Spacer(Modifier.height(24.dp))
        ResendLine(
            state = state,
            colors = colors,
            onResend = onResend,
        )
        Spacer(Modifier.height(16.dp))
        StatusMessage(
            status = state.status,
            colors = colors,
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onSubmit,
            enabled = state.isComplete,
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
            Text(
                text = stringResource(R.string.submit),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ResendLine(
    state: OtpState,
    colors: OtpColors,
    onResend: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.didnt_receive_code) + " ",
            color = colors.textSecondary,
            fontSize = 14.sp,
        )
        TextButton(
            onClick = onResend,
            enabled = state.canResend,
            modifier = Modifier.testTag("otp_resend"),
        ) {
            val label = if (state.canResend) {
                stringResource(R.string.resend)
            } else {
                stringResource(R.string.resend_in, state.resendSecondsLeft)
            }
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = if (state.canResend) colors.link else colors.textSecondary)) {
                        append(label)
                    }
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun StatusMessage(
    status: OtpStatus,
    colors: OtpColors,
) {
    val (text, color) = when (status) {
        OtpStatus.Idle -> "" to colors.textSecondary
        OtpStatus.Success -> stringResource(R.string.success_message) to colors.success
        OtpStatus.Error -> stringResource(R.string.error_message) to colors.error
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
