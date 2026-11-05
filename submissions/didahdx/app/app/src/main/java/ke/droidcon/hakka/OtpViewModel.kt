package ke.droidcon.hakka

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** One state owner survives activity recreation. OTPs are intentionally not persisted to disk. */
class OtpViewModel(private val now: () -> Long = { SystemClock.elapsedRealtime() }) : ViewModel() {
    private var resendDeadline = now() + RESEND_MILLIS
    private val mutableState = MutableStateFlow(OtpState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                refreshCountdown()
                delay(100)
            }
        }
    }

    fun input(index: Int, text: String): Int {
        mutableState.update { OtpLogic.input(it, index, text) }
        return mutableState.value.focus
    }
    fun backspace(index: Int): Int {
        mutableState.update { OtpLogic.backspace(it, index) }
        return mutableState.value.focus
    }
    fun focus(index: Int) { mutableState.update { it.copy(focus = index) } }
    fun submit() { mutableState.update(OtpLogic::submit) }

    fun refreshCountdown() {
        mutableState.update { it.copy(secondsRemaining = OtpLogic.remaining(resendDeadline, now())) }
    }

    fun resend() {
        val time = now()
        if (time < resendDeadline) return
        resendDeadline = time + RESEND_MILLIS
        mutableState.update { it.copy(secondsRemaining = 30, verification = Verification.Idle) }
    }
}
