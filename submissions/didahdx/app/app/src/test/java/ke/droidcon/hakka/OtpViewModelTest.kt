package ke.droidcon.hakka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OtpViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun resendCannotRestartUntilDeadlineAndRepeatedTapsAreIgnored() {
        var time = 0L
        val model = OtpViewModel { time }
        time = 10_000; model.resend(); model.refreshCountdown()
        assertEquals(20, model.state.value.secondsRemaining)
        time = 30_000; model.refreshCountdown()
        assertEquals(0, model.state.value.secondsRemaining)
        model.resend()
        assertEquals(30, model.state.value.secondsRemaining)
        time = 31_000; model.resend(); model.refreshCountdown()
        assertEquals(29, model.state.value.secondsRemaining)
    }
    @Test fun elapsedTimeDrivesCountdownAfterSuspensionAndInputIsRetained() {
        var time = 0L
        val model = OtpViewModel { time }
        model.input(0, "21")
        time = 65_000; model.refreshCountdown()
        assertEquals(0, model.state.value.secondsRemaining)
        assertEquals("21", model.state.value.digits.joinToString(""))
        assertEquals(2, model.state.value.focus)
    }
}
