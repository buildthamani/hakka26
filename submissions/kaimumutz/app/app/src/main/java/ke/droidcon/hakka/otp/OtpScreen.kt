package ke.droidcon.hakka.otp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ke.droidcon.hakka.HakkaTheme
import ke.droidcon.hakka.R

/** Holds the single [OtpState] for the screen; every change goes through [update]. */
@Stable
class OtpStateHolder(initial: OtpState = OtpState()) {
    var state by mutableStateOf(initial)
        private set

    fun update(transform: OtpState.() -> OtpState) {
        state = state.transform()
    }
}

/** OTP verification shown as a bottom sheet over the dimmed, blurred app. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpScreen(
    verifier: OtpVerifier = remember { DemoOtpVerifier() },
    holder: OtpStateHolder = remember { OtpStateHolder() },
) {
    val colors = rememberOtpColors()
    val state = holder.state
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    fun focusInput() {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    // Open the numeric keyboard as soon as the sheet appears.
    LaunchedEffect(Unit) { focusInput() }

    // Real one-second countdown for the resend link.
    LaunchedEffect(state.resendSecondsLeft) {
        if (state.resendSecondsLeft > 0) {
            delay(1_000)
            holder.update { tick() }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        BackgroundContent()
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.scrim),
        )
        Box(
            Modifier
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            OtpSheet(
                state = state,
                colors = colors,
                focusRequester = focusRequester,
                onType = { holder.update { typeDigit(it) } },
                onBackspace = { holder.update { backspace() } },
                onPaste = { holder.update { paste(it) } },
                onBoxClick = { index ->
                    holder.update { focus(index) }
                    focusInput()
                },
                onSubmit = {
                    holder.update { submit(verifier) }
                    if (holder.state.status == OtpStatus.Error) focusInput() else keyboard?.hide()
                },
                onResend = { holder.update { resend() } },
            )
        }
    }
}

@Composable
private fun OtpSheet(
    state: OtpState,
    colors: OtpColors,
    focusRequester: FocusRequester,
    onType: (Char) -> Unit,
    onBackspace: () -> Unit,
    onPaste: (String) -> Unit,
    onBoxClick: (Int) -> Unit,
    onSubmit: () -> Unit,
    onResend: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.86f)
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(colors.sheet)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            Image(
                painter = painterResource(R.drawable.thamani_logo),
                contentDescription = stringResource(R.string.otp_logo_description),
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.otp_title),
                color = colors.textPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.otp_subtitle),
                color = colors.textSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 260.dp),
            )
            Spacer(Modifier.height(32.dp))
            OtpInput(
                state = state,
                colors = colors,
                focusRequester = focusRequester,
                onType = onType,
                onBackspace = onBackspace,
                onPaste = onPaste,
                onBoxClick = onBoxClick,
            )
            Spacer(Modifier.height(16.dp))
            StatusMessage(state.status, colors)
            Spacer(Modifier.height(8.dp))
            ResendLine(state.resendSecondsLeft, state.canResend, colors, onResend)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            enabled = state.canSubmit,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.buttonBg,
                contentColor = colors.buttonText,
                disabledContainerColor = colors.buttonDisabledBg,
                disabledContentColor = colors.buttonText.copy(alpha = 0.75f),
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

/**
 * The six digit circles. Keystrokes go through one invisible text field that always holds a single
 * sentinel character: deleting it is a backspace (even when the focused box is empty), and anything
 * typed or pasted is reduced to its digits. The boxes themselves render [OtpState] only.
 */
@Composable
fun OtpInput(
    state: OtpState,
    colors: OtpColors,
    focusRequester: FocusRequester,
    onType: (Char) -> Unit,
    onBackspace: () -> Unit,
    onPaste: (String) -> Unit,
    onBoxClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val pasteFromClipboard: () -> Unit = {
        scope.launch {
            val text = clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)?.text?.toString()
            if (text != null) onPaste(text)
        }
    }

    Box(modifier.widthIn(max = 360.dp), contentAlignment = Alignment.Center) {
        BasicTextField(
            value = SentinelValue,
            onValueChange = { value ->
                val text = value.text
                if (text == SENTINEL) return@BasicTextField
                if (text.isEmpty()) return@BasicTextField onBackspace()
                val digits = text.filter(Char::isDigit)
                when {
                    digits.length == 1 -> onType(digits.single())
                    digits.length > 1 -> onPaste(text)
                    // No digits: rejected.
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            singleLine = true,
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(OTP_LENGTH) { index ->
                DigitCircle(
                    position = index + 1,
                    digit = state.digits[index],
                    digitState = state.digitState(index),
                    colors = colors,
                    onClick = { onBoxClick(index) },
                    onLongClick = pasteFromClipboard,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DigitCircle(
    position: Int,
    digit: Char?,
    digitState: DigitState,
    colors: OtpColors,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (background, outline, textColor) = when (digitState) {
        DigitState.Filled -> Triple(colors.digitFilledBg, Color.Transparent, colors.digitFilledText)
        DigitState.Focused -> Triple(colors.digitFocusedBg, colors.digitFocusedOutline, colors.textPrimary)
        DigitState.Empty -> Triple(colors.digitEmptyBg, colors.digitEmptyOutline, colors.digitPlaceholder)
    }
    val outlineWidth = if (digitState == DigitState.Focused) 1.5.dp else 1.dp
    val label = stringResource(R.string.otp_digit_description, position, OTP_LENGTH)
    val value = digit?.toString() ?: stringResource(R.string.otp_digit_empty)
    // Empty boxes show a dash placeholder, never a real "0".
    val shown = digit?.toString() ?: if (digitState == DigitState.Empty) PLACEHOLDER else ""

    Box(
        modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(background)
            .border(outlineWidth, outline, CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("otp_digit_$position")
            .clearAndSetSemantics {
                contentDescription = label
                stateDescription = value
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(shown, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusMessage(status: OtpStatus, colors: OtpColors) {
    val (text, color) = when (status) {
        OtpStatus.Success -> stringResource(R.string.otp_success) to colors.success
        OtpStatus.Error -> stringResource(R.string.otp_error) to colors.error
        OtpStatus.None -> "" to Color.Unspecified
    }
    Text(
        text = text,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("otp_status")
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun ResendLine(secondsLeft: Int, canResend: Boolean, colors: OtpColors, onResend: () -> Unit) {
    val prompt = stringResource(R.string.otp_resend_prompt)
    val action = stringResource(R.string.otp_resend_action)
    val countdown = stringResource(R.string.otp_resend_countdown, formatCountdown(secondsLeft))
    val text = buildAnnotatedString {
        if (canResend) {
            withStyle(SpanStyle(color = colors.textSecondary)) { append("$prompt ") }
            withStyle(SpanStyle(color = colors.link, fontWeight = FontWeight.SemiBold)) { append(action) }
        } else {
            withStyle(SpanStyle(color = colors.textSecondary)) { append(countdown) }
        }
    }
    Text(
        text = text,
        style = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Center),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = canResend, role = Role.Button, onClick = onResend)
            .testTag("otp_resend")
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

/** Stand-in for the app screen behind the sheet, blurred on Android 12+. */
@Composable
private fun BackgroundContent() {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .blur(6.dp)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.otp_background_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(24.dp))
        repeat(3) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.height(12.dp))
        }
        Box(
            Modifier
                .width(160.dp)
                .height(12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

private const val SENTINEL = " "
private const val PLACEHOLDER = "–"
private val SentinelValue = TextFieldValue(SENTINEL, TextRange(SENTINEL.length))

@Preview(name = "Partially filled")
@Composable
private fun OtpScreenPreview() {
    HakkaTheme {
        OtpScreen(holder = remember { OtpStateHolder(OtpState().paste("215").focus(2)) })
    }
}

@Preview(name = "Partially filled, dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OtpScreenDarkPreview() {
    OtpScreenPreview()
}
