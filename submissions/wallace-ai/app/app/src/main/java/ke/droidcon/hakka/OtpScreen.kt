package ke.droidcon.hakka

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun OtpScreen() {
    val isDark = isSystemInDarkTheme()
    val sheetBg = if (isDark) sheetDark else sheetLight
    val scrim = if (isDark) scrimDark else scrimLight
    val textPrimary = if (isDark) textPrimaryDark else textPrimaryLight
    val textSecondary = if (isDark) textSecondaryDark else textSecondaryLight
    val buttonBg = if (isDark) buttonBgDark else buttonBgLight
    val buttonText = if (isDark) buttonTextDark else buttonTextLight
    val buttonDisabledBg = if (isDark) buttonDisabledBgDark else buttonDisabledBgLight
    val linkColor = if (isDark) linkDark else linkLight
    val successColor = if (isDark) successDark else successLight
    val errorColor = if (isDark) errorDark else errorLight

    var otpText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    var countdown by remember { mutableStateOf(30) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrim),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(sheetBg)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.thamani_logo),
                contentDescription = "Logo",
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enter your code",
                color = textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "A 6-digit code has been sent to your phone.",
                color = textSecondary,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            OtpInputBoxes(
                otpText = otpText,
                onOtpChange = {
                    otpText = it
                    if (isError) {
                        isError = false
                        statusMessage = ""
                    }
                },
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    color = if (isSuccess) successColor else errorColor,
                    fontSize = 14.sp,
                    modifier = Modifier.testTag("otp_status")
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (countdown > 0) {
                    Text(
                        text = "Didn't receive the code? ",
                        color = textSecondary,
                        fontSize = 14.sp
                    )
                    val seconds = countdown.toString().padStart(2, '0')
                    Text(
                        text = "Resend in 0:$seconds",
                        color = textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("otp_resend")
                    )
                } else {
                    Text(
                        text = "Didn't receive the code? ",
                        color = textSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Resend",
                        color = linkColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                countdown = 30
                            }
                            .testTag("otp_resend")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (otpText == "123456") {
                        isSuccess = true
                        isError = false
                        statusMessage = "You're in."
                    } else {
                        isSuccess = false
                        isError = true
                        statusMessage = "That code didn't work. Try again."
                        otpText = ""
                    }
                },
                enabled = otpText.length == 6,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBg,
                    contentColor = buttonText,
                    disabledContainerColor = buttonDisabledBg,
                    disabledContentColor = buttonText.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("otp_submit")
            ) {
                Text("Submit", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OtpInputBoxes(
    otpText: String,
    onOtpChange: (String) -> Unit,
    isDark: Boolean
) {
    
    val focusRequesters = remember { List(6) { FocusRequester() } }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 6) {
            val char = otpText.getOrNull(i)?.toString() ?: ""
            val isFocused = otpText.length == i || (otpText.length == 6 && i == 5)
            val isFilled = char.isNotEmpty()

            val bg = when {
                isFilled -> if (isDark) digitFilledBgDark else digitFilledBgLight
                isFocused -> if (isDark) digitFocusedBgDark else digitFocusedBgLight
                else -> if (isDark) digitEmptyBgDark else digitEmptyBgLight
            }
            val borderCol = when {
                isFocused -> if (isDark) digitFocusedOutlineDark else digitFocusedOutlineLight
                !isFilled -> if (isDark) digitEmptyOutlineDark else digitEmptyOutlineLight
                else -> Color.Transparent
            }
            val textCol = if (isFilled) {
                if (isDark) digitFilledTextDark else digitFilledTextLight
            } else {
                if (isDark) digitPlaceholderDark else digitPlaceholderLight
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(bg)
                    .border(if (isFocused || !isFilled) 1.dp else 0.dp, borderCol, CircleShape)
                    .semantics {
                        contentDescription = "Digit ${i + 1} of 6"
                    }
                    .testTag("otp_digit_${i + 1}")
            ) {
                BasicTextField(
                    value = char,
                    onValueChange = { newValue ->
                        // Handle pasting
                        if (newValue.length > 1) {
                            val digits = newValue.filter { it.isDigit() }
                            if (digits.isNotEmpty()) {
                                // Find if pasting something like "Your code is 482913"
                                val newOtp = (otpText.take(i) + digits).take(6)
                                onOtpChange(newOtp)
                                val focusIndex = minOf(newOtp.length, 5)
                                focusRequesters[focusIndex].requestFocus()
                            }
                        } else if (newValue.length == 1 && newValue[0].isDigit()) {
                            val newOtp = otpText.take(i) + newValue + otpText.drop(i + 1)
                            onOtpChange(newOtp.take(6))
                            if (i < 5) focusRequesters[i + 1].requestFocus()
                        } else if (newValue.isEmpty()) {
                            val newOtp = otpText.take(i) + otpText.drop(i + 1)
                            onOtpChange(newOtp)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = TextStyle(
                        color = textCol,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(Color.Transparent),
                    modifier = Modifier
                        .focusRequester(focusRequesters[i])
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace) {
                                if (char.isEmpty() && i > 0) {
                                    val newOtp = otpText.take(i - 1) + otpText.drop(i)
                                    onOtpChange(newOtp)
                                    focusRequesters[i - 1].requestFocus()
                                    return@onKeyEvent true
                                }
                            }
                            false
                        }
                )

                if (char.isEmpty()) {
                    Text(
                        text = "-",
                        color = textCol,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Auto focus the appropriate box when otpText changes, e.g. on error clear
    LaunchedEffect(otpText) {
        val focusIndex = minOf(otpText.length, 5)
        focusRequesters[focusIndex].requestFocus()
    }
}
