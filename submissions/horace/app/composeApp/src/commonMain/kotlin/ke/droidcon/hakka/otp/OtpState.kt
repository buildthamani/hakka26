package ke.droidcon.hakka.otp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** On-screen copy, in one place. */
object OtpCopy {
    const val TITLE = "Enter your code"
    const val SUBTITLE = "A 6-digit code has been sent to your phone."
    const val RESEND_PROMPT = "Didn't receive the code? "
    const val RESEND = "Resend"
    const val SUBMIT = "Submit"
    const val SUCCESS = "You're in."
    const val ERROR = "That code didn't work. Try again."

    fun resendIn(seconds: Int): String = "Resend in ${formatCountdown(seconds)}"
    fun digitLabel(index: Int, length: Int): String = "Digit ${index + 1} of $length"
}

sealed interface OtpStatus {
    data object Idle : OtpStatus
    data object Success : OtpStatus
    data object Error : OtpStatus
}

/** Formats seconds as `M:SS`, e.g. 30 -> "0:30", 5 -> "0:05". */
fun formatCountdown(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/**
 * Works out what the user added to a box that held [old] and now holds [new]
 * (the old digit may sit before or after the new input, depending on the cursor).
 */
fun insertedText(old: String, new: String): String = when {
    old.isEmpty() -> new
    new.startsWith(old) -> new.drop(old.length)
    new.endsWith(old) -> new.dropLast(old.length)
    else -> new
}

/**
 * Single source of truth for the OTP sheet: the digits, which box has focus,
 * the submit result and the resend countdown. The UI only renders this and forwards events.
 */
class OtpState(
    val length: Int = DEFAULT_LENGTH,
    private val resendCooldownSeconds: Int = DEFAULT_RESEND_SECONDS,
    private val verify: (String) -> Boolean,
) {
    private val _digits = mutableStateListOf<String>().apply { repeat(length) { add("") } }
    val digits: List<String> get() = _digits

    var focusedIndex by mutableIntStateOf(0)
        private set

    /** Bumped whenever the state (not the user) wants focus moved, so the UI can follow. */
    var focusRequest by mutableIntStateOf(0)
        private set

    var status by mutableStateOf<OtpStatus>(OtpStatus.Idle)
        private set

    var resendSecondsRemaining by mutableIntStateOf(resendCooldownSeconds)
        private set

    /** Bumped on every resend so the UI restarts its ticker. */
    var resendCount by mutableIntStateOf(0)
        private set

    val code: String get() = digits.joinToString("")
    val isComplete: Boolean get() = digits.all { it.isNotEmpty() }
    val canResend: Boolean get() = resendSecondsRemaining == 0

    /** Handles typed or pasted text in box [index]. Non-digits are ignored. */
    fun enter(index: Int, text: String) {
        val incoming = text.filter { it in '0'..'9' }
        if (incoming.isEmpty() || index !in 0 until length) return
        if (status == OtpStatus.Error) status = OtpStatus.Idle

        // A full code pasted anywhere fills every box from the start.
        val start = if (incoming.length >= length) 0 else index
        val chunk = incoming.take(length - start)
        chunk.forEachIndexed { offset, c -> _digits[start + offset] = c.toString() }
        moveFocus((start + chunk.length).coerceAtMost(length - 1))
    }

    /** Backspace in box [index]: clears it, or if already empty, clears the previous box and moves there. */
    fun backspace(index: Int) {
        if (index !in 0 until length) return
        if (digits[index].isNotEmpty()) {
            _digits[index] = ""
            if (focusedIndex != index) moveFocus(index)
        } else if (index > 0) {
            _digits[index - 1] = ""
            moveFocus(index - 1)
        }
    }

    /** Called when the user focuses a box directly (e.g. by tapping it). */
    fun onBoxFocused(index: Int) {
        if (index in 0 until length) focusedIndex = index
    }

    fun submit() {
        if (!isComplete) return
        if (verify(code)) {
            status = OtpStatus.Success
        } else {
            status = OtpStatus.Error
            for (i in _digits.indices) _digits[i] = ""
            moveFocus(0)
        }
    }

    /** One second of the resend countdown has passed. */
    fun tick() {
        if (resendSecondsRemaining > 0) resendSecondsRemaining--
    }

    fun resend() {
        if (!canResend) return
        resendSecondsRemaining = resendCooldownSeconds
        resendCount++
    }

    private fun moveFocus(index: Int) {
        focusedIndex = index
        focusRequest++
    }

    companion object {
        const val DEFAULT_LENGTH = 6
        const val DEFAULT_RESEND_SECONDS = 30
    }
}

/** Stand-in for a backend check: this demo accepts a single known code. */
object DemoOtpVerifier {
    private const val VALID_CODE = "123456"
    fun verify(code: String): Boolean = code == VALID_CODE
}
