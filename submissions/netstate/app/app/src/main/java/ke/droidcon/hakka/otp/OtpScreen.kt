package ke.droidcon.hakka.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpBottomSheet(
    state: OtpState,
    onDigitsChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    onTick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.5f)
    ) {
        OtpScreenContent(
            state = state,
            onDigitsChanged = onDigitsChanged,
            onSubmit = onSubmit,
            onResend = onResend,
            onTick = onTick
        )
    }
}

@Composable
fun OtpScreenContent(
    state: OtpState,
    onDigitsChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    onTick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Timer effect for Resend countdown
    LaunchedEffect(state.secondsLeft) {
        if (state.secondsLeft > 0) {
            delay(1000L)
            onTick()
        }
    }

    // Auto-focus the hidden input on launch
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Re-focus and show keyboard if an error occurs (digits clear out)
    LaunchedEffect(state.status) {
        if (state.status == OtpStatus.Error) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(ke.droidcon.hakka.R.drawable.thamani_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Enter your code",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "A 6-digit code has been sent to your phone.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Hidden BasicTextField driving the UI to seamlessly support pasting and native keyboard behavior
        BasicTextField(
            value = state.digits,
            onValueChange = onDigitsChanged,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.focusRequester(focusRequester),
            decorationBox = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 6) {
                        val isFilled = i < state.digits.length
                        val isFocused = i == state.activeIndex && state.status != OtpStatus.Success
                        val digitChar = if (isFilled) state.digits[i].toString() else ""

                        val bgColor = when {
                            isFilled -> Color(0xFF1B5E20)
                            else -> Color.Transparent
                        }

                        val textColor = when {
                            isFilled -> Color.White
                            else -> Color.Transparent
                        }

                        val borderColor = when {
                            isFilled -> Color(0xFF1B5E20)
                            isFocused -> MaterialTheme.colorScheme.onSurface // Dark outline
                            else -> Color.LightGray
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(bgColor)
                                .border(2.dp, borderColor, CircleShape)
                                .semantics {
                                    testTag = "otp_digit_${i + 1}"
                                    contentDescription = "Digit ${i + 1} of 6"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFilled) {
                                Text(
                                    text = digitChar,
                                    color = textColor,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        val statusText = when (state.status) {
            OtpStatus.Success -> "You're in."
            OtpStatus.Error -> "That code didn't work. Try again."
            OtpStatus.Idle -> ""
        }

        Text(
            text = statusText,
            color = if (state.status == OtpStatus.Error) Color.Red else Color(0xFF1B5E20),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier
                .semantics { testTag = "otp_status" }
                .heightIn(min = 20.dp) // Prevent UI jitter when text appears
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Resend Text
        val resendText = if (state.canResend) {
            "Didn't receive the code? Resend"
        } else {
            "Resend in 0:${state.secondsLeft.toString().padStart(2, '0')}"
        }

        Text(
            text = resendText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (state.canResend) Color(0xFF1B5E20) else Color.Gray,
            modifier = Modifier
                .clickable(enabled = state.canResend, onClick = onResend)
                .padding(8.dp) // Minimum touch target padding
                .semantics { testTag = "otp_resend" }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Submit Button
        Button(
            onClick = onSubmit,
            enabled = state.isSubmitEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics { testTag = "otp_submit" },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1B5E20),
                disabledContainerColor = Color.LightGray
            )
        ) {
            Text("Submit")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun OtpScreenPreview() {
    MaterialTheme {
        OtpScreenContent(
            state = OtpState(digits = "123", secondsLeft = 30),
            onDigitsChanged = {},
            onSubmit = {},
            onResend = {},
            onTick = {}
        )
    }
}
