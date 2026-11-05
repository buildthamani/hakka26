package ke.droidcon.hakka

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { OtpScreen() } }
    }
}

// A hidden editing anchor lets software keyboards report deletion even in an empty box.
private const val Anchor = "\u200B"
private val HideAnchor = VisualTransformation { value ->
    TransformedText(AnnotatedString(value.text.removePrefix(Anchor)), object : OffsetMapping {
        override fun originalToTransformed(offset: Int) = (offset - 1).coerceAtLeast(0)
        override fun transformedToOriginal(offset: Int) = (offset + 1).coerceAtMost(value.length)
    })
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun OtpScreen(dark: Boolean = isSystemInDarkTheme()) {
    val palette = if (dark) DarkOtp else LightOtp
    var digits by rememberSaveable { mutableStateOf(List(6) { "" }) }
    var status by rememberSaveable { mutableStateOf("") }
    var focus by rememberSaveable { mutableIntStateOf(0) }
    var focusRequest by remember { mutableIntStateOf(0) }
    var deadline by rememberSaveable { mutableLongStateOf(SystemClock.elapsedRealtime() + 30_000) }
    var remaining by remember { mutableIntStateOf(remainingSeconds(deadline, SystemClock.elapsedRealtime())) }
    val requesters = remember { List(6) { FocusRequester() } }
    val keyboard = LocalSoftwareKeyboardController.current
    fun apply(next: OtpState) {
        digits = next.digits
        status = next.status
        focus = next.focus
        focusRequest++
    }
    LaunchedEffect(focusRequest) {
        requesters[focus].requestFocus()
        keyboard?.show()
    }
    LaunchedEffect(deadline) {
        do {
            remaining = remainingSeconds(deadline, SystemClock.elapsedRealtime())
            if (remaining > 0) delay(200)
        } while (remaining > 0)
    }
    Box(Modifier.fillMaxSize().background(Color(0xFFE6E9E7)).semantics { testTagsAsResourceId = true }) {
        Box(Modifier.fillMaxSize().background(palette.scrim))
        Surface(
            color = palette.sheet,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.86f)
                .imePadding(),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
            val availableHeight = maxHeight
            Column(
                Modifier.verticalScroll(rememberScrollState()).heightIn(min = availableHeight).navigationBarsPadding().padding(horizontal = 20.dp).padding(top = 32.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painterResource(if (dark) R.drawable.thamani_logo_white else R.drawable.thamani_logo),
                    contentDescription = "Thamani logo", modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(18.dp))
                Text("Enter your code", color = palette.primary, fontSize = 26.sp)
                Spacer(Modifier.height(10.dp))
                Text("A 6-digit code has been sent to your phone.", color = palette.secondary,
                    fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 260.dp))
                Spacer(Modifier.height(36.dp))
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val diameter = ((maxWidth - 30.dp) / 6).coerceAtMost(52.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                        repeat(6) { index ->
                            val filled = digits[index].isNotEmpty()
                            val selected = focus == index
                            val background = if (filled) palette.filled else if (selected) palette.sheet else palette.empty
                            BasicTextField(
                                value = TextFieldValue(Anchor + digits[index], TextRange(1 + digits[index].length)),
                                onValueChange = { value ->
                                    val state = OtpState(digits, focus, status)
                                    when {
                                        value.text.isEmpty() || (filled && value.text == Anchor) -> apply(state.backspace(index))
                                        value.text.filter { it in '0'..'9' }.isNotEmpty() -> {
                                            val raw = value.text.removePrefix(Anchor)
                                            // Appending to an existing digit replaces it; full pasted codes start at box one.
                                            val input = if (filled && raw.length == 2 && raw.startsWith(digits[index])) raw.takeLast(1) else raw
                                            apply(state.input(index, input))
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                singleLine = true,
                                visualTransformation = HideAnchor,
                                cursorBrush = SolidColor(Color.Transparent),
                                textStyle = TextStyle(color = if (filled) Color.White else palette.primary, fontSize = 20.sp, textAlign = TextAlign.Center),
                                modifier = Modifier.size(diameter).focusRequester(requesters[index])
                                    .onFocusChanged { if (it.isFocused) focus = index }
                                    .onPreviewKeyEvent {
                                        if (it.type == KeyEventType.KeyDown && it.key == Key.Backspace) {
                                            apply(OtpState(digits, focus, status).backspace(index)); true
                                        } else false
                                    }
                                    .testTag("otp_digit_${index + 1}")
                                    .semantics { contentDescription = "Digit ${index + 1} of 6" }
                                    .background(background, CircleShape)
                                    .border(if (selected || !filled) 1.dp else 0.dp,
                                        if (selected) palette.primary else if (!filled) palette.outline else background, CircleShape),
                                decorationBox = { field ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (!filled) Text("•", color = palette.placeholder, fontSize = 20.sp)
                                        field()
                                    }
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Didn't receive the code?", color = palette.secondary, fontSize = 12.sp)
                    TextButton(
                        enabled = remaining == 0,
                        onClick = { deadline = SystemClock.elapsedRealtime() + 30_000; remaining = 30 },
                        modifier = Modifier.testTag("otp_resend"),
                        colors = ButtonDefaults.textButtonColors(contentColor = palette.link, disabledContentColor = palette.secondary),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) { Text(if (remaining > 0) "Resend in 0:${remaining.toString().padStart(2, '0')}" else "Resend", fontSize = 12.sp) }
                }
                Text(status, color = if (status == "You're in.") palette.success else palette.error,
                    textAlign = TextAlign.Center, modifier = Modifier.testTag("otp_status").semantics { liveRegion = LiveRegionMode.Polite })
                Spacer(Modifier.weight(1f).heightIn(min = 36.dp))
                Button(
                    onClick = { apply(OtpState(digits, focus, status).submit()) },
                    enabled = digits.all { it.length == 1 },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("otp_submit"),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.filled, contentColor = Color.White,
                        disabledContainerColor = palette.disabled, disabledContentColor = Color.White),
                ) { Text("Submit") }
            }
            }
        }
    }
}
