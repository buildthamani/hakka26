package ke.droidcon.hakka.ui

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.droidcon.hakka.R
import ke.droidcon.hakka.otp.OtpConfig
import ke.droidcon.hakka.otp.OtpStatus
import ke.droidcon.hakka.otp.OtpUiState
import ke.droidcon.hakka.ui.theme.LocalOtpColours
import ke.droidcon.hakka.ui.theme.OtpColours
import ke.droidcon.hakka.ui.theme.OtpTheme

/**
 * Stateless OTP verification screen. State and behaviour live in OtpViewModel; this
 * composable only renders [state] and raises user intent through callbacks.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpScreen(
    state: OtpUiState,
    onDigitChange: (Int, String) -> Unit,
    onBackspace: (Int) -> Unit,
    onFocusChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = LocalOtpColours.current
    val focusRequesters = remember {
        List(OtpConfig.LENGTH) { FocusRequester() }
    }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.focusedIndex, state.isVerifying) {
        if (state.isVerifying) {
            keyboard?.hide()
        } else {
            focusRequesters[state.focusedIndex.coerceIn(0, OtpConfig.LENGTH - 1)]
                .requestFocus()
            keyboard?.show()
        }
    }

    LaunchedEffect(state.status) {
        if (state.status == OtpStatus.Success) {
            keyboard?.hide()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        DimmedBackdrop(colours)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colours.scrim),
        )
        OtpSheet(
            state = state,
            colours = colours,
            focusRequesters = focusRequesters,
            onDigitChange = onDigitChange,
            onBackspace = onBackspace,
            onFocusChange = onFocusChange,
            onSubmit = onSubmit,
            onResend = onResend,
            onBack = onBack,
        )
    }
}

@Composable
private fun DimmedBackdrop(colours: OtpColours) {
    val blur = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(18.dp)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colours.sheet)
            .then(blur),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.padding(top = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(logoResource(colours.isDark)),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.otp_backdrop_title),
                color = colours.textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun OtpSheet(
    state: OtpUiState,
    colours: OtpColours,
    focusRequesters: List<FocusRequester>,
    onDigitChange: (Int, String) -> Unit,
    onBackspace: (Int) -> Unit,
    onFocusChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .background(
                color = colours.sheet,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            )
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DragHandle(colours)

        Header(
            colours = colours,
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.otp_title),
                color = colours.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.otp_subtitle),
                color = colours.textSecondary,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            OtpDigits(
                state = state,
                colours = colours,
                focusRequesters = focusRequesters,
                onDigitChange = onDigitChange,
                onBackspace = onBackspace,
                onFocusChange = onFocusChange,
            )
            Spacer(Modifier.height(20.dp))
            ResendLine(
                state = state,
                colours = colours,
                onResend = onResend,
            )
            Spacer(Modifier.height(12.dp))
            StatusMessage(
                status = state.status,
                colours = colours,
            )
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = onSubmit,
            enabled = state.canSubmit,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colours.buttonBackground,
                contentColor = colours.buttonText,
                disabledContainerColor = colours.buttonDisabledBackground,
                disabledContentColor = colours.buttonText,
            ),
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .fillMaxWidth()
                .height(56.dp)
                .testTag("otp_submit"),
        ) {
            Text(
                text = stringResource(R.string.otp_submit),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DragHandle(colours: OtpColours) {
    Box(
        modifier = Modifier
            .padding(top = 12.dp)
            .width(36.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(colours.digitEmptyOutline),
    )
}

@Composable
private fun Header(
    colours: OtpColours,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.otp_back),
                tint = colours.textPrimary,
            )
        }
        Image(
            painter = painterResource(logoResource(colours.isDark)),
            contentDescription = stringResource(R.string.otp_logo_content_description),
            modifier = Modifier
                .align(Alignment.Center)
                .size(52.dp),
        )
    }
}

@Composable
private fun OtpDigits(
    state: OtpUiState,
    colours: OtpColours,
    focusRequesters: List<FocusRequester>,
    onDigitChange: (Int, String) -> Unit,
    onBackspace: (Int) -> Unit,
    onFocusChange: (Int) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val spacing = 8.dp
        val available = maxWidth - spacing * (OtpConfig.LENGTH - 1)
        val boxSize = (available / OtpConfig.LENGTH.toFloat()).coerceAtMost(56.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                spacing,
                Alignment.CenterHorizontally,
            ),
        ) {
            repeat(OtpConfig.LENGTH) { index ->
                OtpDigitBox(
                    index = index,
                    digit = state.digits[index],
                    isFocused = state.focusedIndex == index,
                    isEnabled = !state.isVerifying,
                    size = boxSize,
                    colours = colours,
                    focusRequester = focusRequesters[index],
                    onValueChange = { onDigitChange(index, it) },
                    onBackspace = { onBackspace(index) },
                    onFocusChange = { focused ->
                        if (focused) onFocusChange(index)
                    },
                )
            }
        }
    }
}

@Composable
private fun OtpDigitBox(
    index: Int,
    digit: String,
    isFocused: Boolean,
    isEnabled: Boolean,
    size: Dp,
    colours: OtpColours,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    val hasDigit = digit.isNotEmpty()
    val background = when {
        hasDigit -> colours.digitFilledBackground
        isFocused -> colours.digitFocusedBackground
        else -> colours.digitEmptyBackground
    }
    val outline = when {
        hasDigit -> colours.digitFilledBackground
        isFocused -> colours.digitFocusedOutline
        else -> colours.digitEmptyOutline
    }
    val borderWidth = if (isFocused && !hasDigit) 2.dp else 1.dp
    val digitDescription = stringResource(
        R.string.otp_digit_content_description,
        index + 1,
        OtpConfig.LENGTH,
    )
    val emptyDescription = stringResource(R.string.otp_digit_empty)

    BasicTextField(
        value = digit.ifEmpty { OtpConfig.EMPTY_DIGIT_TEXT },
        onValueChange = onValueChange,
        enabled = isEnabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onNext = {}),
        textStyle = TextStyle(
            color = if (hasDigit) colours.digitFilledText else Color.Transparent,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(
            if (hasDigit) Color.Transparent else colours.digitFocusedOutline,
        ),
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background, CircleShape)
            .border(borderWidth, outline, CircleShape)
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .onPreviewKeyEvent { event ->
                if (
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.Backspace &&
                    digit.isEmpty()
                ) {
                    onBackspace()
                    true
                } else {
                    false
                }
            }
            .testTag("otp_digit_${index + 1}")
            .semantics {
                contentDescription = digitDescription
                stateDescription = if (hasDigit) digit else emptyDescription
            },
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (!hasDigit) {
                    Text(
                        text = "–",
                        color = colours.digitPlaceholder,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun ResendLine(
    state: OtpUiState,
    colours: OtpColours,
    onResend: () -> Unit,
) {
    val label = if (state.canResend) {
        stringResource(R.string.otp_resend_action)
    } else {
        stringResource(R.string.otp_resend_countdown, state.secondsUntilResend)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.otp_resend_prompt),
            color = colours.textSecondary,
            fontSize = 14.sp,
        )
        Spacer(Modifier.width(4.dp))
        TextButton(
            onClick = onResend,
            enabled = state.canResend,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            modifier = Modifier.testTag("otp_resend"),
        ) {
            Text(
                text = label,
                color = if (state.canResend) colours.link else colours.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun StatusMessage(
    status: OtpStatus,
    colours: OtpColours,
) {
    val text = when (status) {
        OtpStatus.Idle -> ""
        OtpStatus.Success -> stringResource(R.string.otp_success)
        OtpStatus.Error -> stringResource(R.string.otp_error)
    }
    val color = when (status) {
        OtpStatus.Idle -> colours.textSecondary
        OtpStatus.Success -> colours.success
        OtpStatus.Error -> colours.error
    }

    Text(
        text = text,
        color = color,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("otp_status")
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

private fun logoResource(isDark: Boolean): Int =
    if (isDark) R.drawable.thamani_logo_white else R.drawable.thamani_logo

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 760)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 760,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun OtpScreenPreview() {
    OtpTheme {
        OtpScreen(
            state = OtpUiState(
                digits = listOf("4", "8", "2", "", "", ""),
                focusedIndex = 3,
                secondsUntilResend = 27,
            ),
            onDigitChange = { _, _ -> },
            onBackspace = { },
            onFocusChange = { },
            onSubmit = { },
            onResend = { },
            onBack = { },
        )
    }
}
