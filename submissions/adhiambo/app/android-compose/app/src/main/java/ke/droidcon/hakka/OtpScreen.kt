package ke.droidcon.hakka

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// An invisible placeholder lets software keyboards report backspace on an empty box.
private const val EmptySlot = "\u200B"

@Composable
fun OtpScreen(nowMillis: () -> Long = { SystemClock.elapsedRealtime() }) {
    var digits by rememberSaveable { mutableStateOf(List(6) { "" }) }
    var active by rememberSaveable { mutableIntStateOf(0) }
    var deadline by rememberSaveable { mutableLongStateOf(0L) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }
    var seconds by remember { mutableIntStateOf(0) }
    val focus = remember { List(6) { FocusRequester() } }
    val complete = digits.all { it.length == 1 }

    LaunchedEffect(active) { focus[active].requestFocus() }
    LaunchedEffect(deadline) {
        while (deadline > 0L) {
            seconds = ((deadline - nowMillis()).coerceAtLeast(0L).plus(999) / 1000).toInt()
            if (seconds == 0) break
            delay(100)
        }
    }

    fun delete(index: Int) {
        digits = digits.toMutableList().also { it[index] = "" }
        active = (index - 1).coerceAtLeast(0)
        message = ""
    }

    fun submit() {
        if (!complete) return
        if (!submitted) {
            seconds = 60
            deadline = nowMillis() + 60_000
            submitted = true
        }
        message = "Code submitted. This is a demo; no verification request was sent."
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 440.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Image(painterResource(R.drawable.thamani_logo), "Thamani", Modifier.size(64.dp))
                Text("Verify your code", style = MaterialTheme.typography.headlineMedium)
                Text("Enter your 6-digit verification code", textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(6) { index ->
                        val isActive = active == index
                        val value = digits[index].ifEmpty { EmptySlot }
                        CompositionLocalProvider(LocalTextSelectionColors provides TextSelectionColors(
                            handleColor = MaterialTheme.colorScheme.primary,
                            backgroundColor = Color.Transparent,
                        )) {
                            BasicTextField(
                                value = TextFieldValue(value, if (index == 5 && digits[index].isNotEmpty())
                                    TextRange(value.length) else TextRange(0, value.length)),
                                onValueChange = { edit ->
                                    val input = edit.text.replace(EmptySlot, "")
                                    when {
                                        edit.text.isEmpty() -> delete(index)
                                        input.isEmpty() -> Unit
                                        input.any { it !in '0'..'9' } -> {
                                            message = "Only numbers are accepted. Paste digits only."
                                        }
                                        else -> {
                                            val accepted = input.take(6 - index)
                                            digits = digits.toMutableList().also { slots ->
                                                accepted.forEachIndexed { offset, char ->
                                                    slots[index + offset] = char.toString()
                                                }
                                            }
                                            active = (index + accepted.length).coerceAtMost(5)
                                            message = ""
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).aspectRatio(1f).testTag("digit-$index")
                                    .focusRequester(focus[index])
                                    .onFocusChanged { if (it.isFocused) active = index }
                                    .onPreviewKeyEvent {
                                        if (it.key == Key.Backspace || it.key == Key.Delete) {
                                            if (it.type == KeyEventType.KeyDown) delete(index)
                                            true
                                        } else false
                                    }
                                    .semantics {
                                        contentDescription = "Digit ${index + 1} of 6"
                                        selected = isActive
                                    },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = if (index == 5) ImeAction.Done else ImeAction.Next,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { submit() },
                                    onNext = { active = (index + 1).coerceAtMost(5) },
                                ),
                                visualTransformation = PasswordVisualTransformation(),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    textAlign = TextAlign.Center,
                                    color = if (digits[index].isEmpty()) Color.Transparent
                                        else MaterialTheme.colorScheme.onSurface,
                                ),
                                decorationBox = { inner ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isActive) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(if (isActive) 2.dp else 1.dp,
                                            if (isActive) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline),
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() }
                                    }
                                },
                            )
                        }
                    }
                }
                Button(onClick = { submit() }, enabled = complete,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("submit")) {
                    Text("Submit")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = {
                        seconds = 60
                        deadline = nowMillis() + 60_000
                        digits = List(6) { "" }
                        active = 0
                        message = "A new demo code was requested. Enter any six digits."
                    }, enabled = submitted && seconds == 0, modifier = Modifier.testTag("resend")) {
                        Text("Resend code")
                    }
                    Text(if (!submitted) "Submit your code to start the retry timer"
                        else if (seconds > 0) "Retry in %d:%02d".format(seconds / 60, seconds % 60)
                        else "You can request a new code now",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("countdown"))
                }
                if (message.isNotEmpty()) Text(message, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("message"))
            }
        }
    }
}
