package ke.droidcon.hakka.otp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OtpViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val OtpViewModel.state get() = uiState.value

    // Digit extraction

    @Test
    fun extractDigitsKeepsOnlyTheDigitsOfPastedText() {
        assertEquals("482913", extractDigits("Your code is 482913"))
    }

    @Test
    fun extractDigitsDropsNonDigitsAndAnythingPastSix() {
        assertEquals("123", extractDigits("1a-2 3."))
        assertEquals("123456", extractDigits("1234567"))
        assertEquals("", extractDigits("no digits"))
    }

    @Test
    fun typingADigitFillsTheBoxAndMovesFocusToTheNext() = runTest(dispatcher) {
        val viewModel = OtpViewModel()
        viewModel.onCodeChange("4")

        assertEquals("4", viewModel.state.code)
        assertEquals(1, viewModel.state.focusedIndex)
    }

    @Test
    fun nonDigitsAreRejected() = runTest(dispatcher) {
        val viewModel = OtpViewModel()
        viewModel.onCodeChange("4")
        viewModel.onCodeChange("4-")

        assertEquals("4", viewModel.state.code)
        assertEquals(1, viewModel.state.focusedIndex)
    }

    @Test
    fun backspaceOnAnEmptyBoxClearsThePreviousDigitAndMovesBack() = runTest(dispatcher) {
        val viewModel = OtpViewModel()
        viewModel.onCodeChange("21")
        assertEquals(2, viewModel.state.focusedIndex)

        viewModel.onCodeChange("2")

        assertEquals("2", viewModel.state.code)
        assertEquals(1, viewModel.state.focusedIndex)
    }

    @Test
    fun pastingAMessageFillsAllSixBoxes() = runTest(dispatcher) {
        val viewModel = OtpViewModel()
        viewModel.onCodeChange("Your code is 482913")

        assertEquals("482913", viewModel.state.code)
        assertTrue(viewModel.state.isComplete)
        assertNull(viewModel.state.focusedIndex)
    }

    // Validation

    @Test
    fun submitDoesNothingUntilAllSixDigitsAreFilled() = runTest(dispatcher) {
        val viewModel = OtpViewModel()
        viewModel.onCodeChange("12345")
        assertFalse(viewModel.state.isComplete)

        viewModel.onSubmit()

        assertEquals(OtpStatus.Idle, viewModel.state.status)
        assertEquals("12345", viewModel.state.code)
    }

    @Test
    fun theValidCodeShowsTheSuccessMessage() = runTest(dispatcher) {
        val viewModel = OtpViewModel()
        viewModel.onCodeChange("123456")
        viewModel.onSubmit()

        assertEquals(OtpStatus.Success, viewModel.state.status)
        assertEquals("You're in.", viewModel.state.statusMessage)
        assertEquals("123456", viewModel.state.code)
    }

    @Test
    fun aWrongCodeShowsTheErrorClearsEveryBoxAndFocusesTheFirst() = runTest(dispatcher) {
        val viewModel = OtpViewModel()
        viewModel.onCodeChange("654321")
        viewModel.onSubmit()

        assertEquals(OtpStatus.Error, viewModel.state.status)
        assertEquals("That code didn't work. Try again.", viewModel.state.statusMessage)
        assertEquals("", viewModel.state.code)
        assertEquals(0, viewModel.state.focusedIndex)
        assertFalse(viewModel.state.isComplete)
    }

    @Test
    fun typingAfterAnErrorClearsTheMessage() = runTest(dispatcher) {
        val viewModel = OtpViewModel()
        viewModel.onCodeChange("654321")
        viewModel.onSubmit()

        viewModel.onCodeChange("9")

        assertEquals(OtpStatus.Idle, viewModel.state.status)
        assertEquals("", viewModel.state.statusMessage)
    }

    // Resend countdown

    @Test
    fun countdownStartsAtThirtySecondsAndTicksDownEverySecond() = runTest(dispatcher) {
        val viewModel = OtpViewModel()
        runCurrent()

        assertEquals(30, viewModel.state.secondsLeft)
        assertEquals("Resend in 0:30", viewModel.state.resendLabel)
        assertFalse(viewModel.state.canResend)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(29, viewModel.state.secondsLeft)
        assertEquals("Resend in 0:29", viewModel.state.resendLabel)

        advanceTimeBy(24_000)
        runCurrent()
        assertEquals("Resend in 0:05", viewModel.state.resendLabel)
    }

    @Test
    fun resendIsIgnoredWhileTheCountdownIsRunning() = runTest(dispatcher) {
        val viewModel = OtpViewModel()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(20, viewModel.state.secondsLeft)

        viewModel.onResend()
        runCurrent()

        assertEquals(20, viewModel.state.secondsLeft)
    }

    @Test
    fun resendBecomesAvailableAfterThirtySecondsAndRestartsTheCountdown() = runTest(dispatcher) {
        val viewModel = OtpViewModel()
        advanceTimeBy(30_000)
        runCurrent()

        assertEquals(0, viewModel.state.secondsLeft)
        assertTrue(viewModel.state.canResend)
        assertEquals("Resend", viewModel.state.resendLabel)

        viewModel.onResend()
        runCurrent()
        assertEquals(30, viewModel.state.secondsLeft)
        assertFalse(viewModel.state.canResend)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(29, viewModel.state.secondsLeft)
    }
}
