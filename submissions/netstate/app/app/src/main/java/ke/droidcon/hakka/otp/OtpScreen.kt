package ke.droidcon.hakka.otp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private data class OtpPalette(
    val sheet: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val digitFilledBg: Color,
    val digitFilledText: Color,
    val digitFocusedBg: Color,
    val digitFocusedOutline: Color,
    val digitEmptyBg: Color,
    val digitEmptyOutline: Color,
    val digitPlaceholder: Color,
    val buttonBg: Color,
    val buttonText: Color,
    val buttonDisabledBg: Color,
    val link: Color,
    val success: Color,
    val error: Color
)

@Composable
private fun otpPalette(): OtpPalette =
    if (isSystemInDarkTheme()) {
        OtpPalette(
            sheet = Color(0xFF161A17),
            textPrimary = Color(0xFFF5F5F5),
            textSecondary = Color(0xFFB0B5B1),
            digitFilledBg = Color(0xFF2E7D45),
            digitFilledText = Color(0xFFFFFFFF),
            digitFocusedBg = Color(0xFF161A17),
            digitFocusedOutline = Color(0xFFF5F5F5),
            digitEmptyBg = Color(0xFF262B27),
            digitEmptyOutline = Color(0xFF353B36),
            digitPlaceholder = Color(0xFF6F7570),
            buttonBg = Color(0xFF2E7D45),
            buttonText = Color(0xFFFFFFFF),
            buttonDisabledBg = Color(0xFF2A302B),
            link = Color(0xFF7BC795),
            success = Color(0xFF7BC795),
            error = Color(0xFFEF9A9A)
        )
    } else {
        OtpPalette(
            sheet = Color(0xFFFFFFFF),
            textPrimary = Color(0xFF111111),
            textSecondary = Color(0xFF5F6368),
            digitFilledBg = Color(0xFF12361B),
            digitFilledText = Color(0xFFFFFFFF),
            digitFocusedBg = Color(0xFFFFFFFF),
            digitFocusedOutline = Color(0xFF111111),
            digitEmptyBg = Color(0xFFF1F1F1),
            digitEmptyOutline = Color(0xFFE0E0E0),
            digitPlaceholder = Color(0xFF9E9E9E),
            buttonBg = Color(0xFF12361B),
            buttonText = Color(0xFFFFFFFF),
            buttonDisabledBg = Color(0xFFC9D1CB),
            link = Color(0xFF12361B),
            success = Color(0xFF1B7F3A),
            error = Color(0xFFC62828)
        )
    }

@Composable
fun OtpScreen(
    state: OtpState,
    onDigitsChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    onTick: () -> Unit,
    onBack: () -> Unit
) {
    val palette = otpPalette()
    BackHandler(onBack = onBack)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = palette.sheet,
        contentColor = palette.textPrimary
    ) {
        OtpScreenContent(
            state = state,
            onDigitsChanged = onDigitsChanged,
            onSubmit = onSubmit,
            onResend = onResend,
            onTick = onTick,
            onBack = onBack
        )
    }
}

@Composable
fun OtpScreenContent(
    state: OtpState,
    onDigitsChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    onTick: () -> Unit,
    onBack: () -> Unit = {}
) {
    val palette = otpPalette()
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
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBack) {
                Text("Back", color = palette.link)
            }
        }

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
            color = palette.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "A 6-digit code has been sent to your phone.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = palette.textSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Hidden BasicTextField driving the UI to seamlessly support pasting and native keyboard behavior
        BasicTextField(
            value = state.digits,
            onValueChange = onDigitsChanged,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.titleLarge.copy(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier.focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 6) {
                            val isFilled = i < state.digits.length
                            val isFocused =
                                i == state.activeIndex && !isFilled && state.status != OtpStatus.Success
                            val digitChar = if (isFilled) state.digits[i].toString() else ""

                            val bgColor = when {
                                isFilled -> palette.digitFilledBg
                                isFocused -> palette.digitFocusedBg
                                else -> palette.digitEmptyBg
                            }

                            val borderColor = when {
                                isFilled -> palette.digitFilledBg
                                isFocused -> palette.digitFocusedOutline
                                else -> palette.digitEmptyOutline
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
                                        color = palette.digitFilledText,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                } else if (state.status != OtpStatus.Success) {
                                    Text(
                                        text = "•",
                                        color = palette.digitPlaceholder,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        }
                    }

                    Box(Modifier.fillMaxSize()) {
                        innerTextField()
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
            color = when (state.status) {
                OtpStatus.Error -> palette.error
                OtpStatus.Success -> palette.success
                OtpStatus.Idle -> palette.textPrimary
            },
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
            color = if (state.canResend) palette.link else palette.textSecondary,
            modifier = Modifier
                .clickable(enabled = state.canResend, onClick = onResend)
                .padding(8.dp) // Minimum touch target padding
                .semantics { testTag = "otp_resend" }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Verify the entered code before proceeding.
        Button(
            onClick = onSubmit,
            enabled = state.isSubmitEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics { testTag = "otp_submit" },
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.buttonBg,
                contentColor = palette.buttonText,
                disabledContainerColor = palette.buttonDisabledBg,
                disabledContentColor = palette.textSecondary
            )
        ) {
            Text("Verify OTP")
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
            onTick = {},
            onBack = {}
        )
    }
}
