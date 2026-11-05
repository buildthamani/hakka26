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

    private fun OtpViewModel.type(code: String) =
        code.forEachIndexed { index, char -> onBoxTextChange(index, char.toString()) }

    private val OtpViewModel.current get() = state.value

    @Test
    fun startsEmptyWithFirstBoxFocusedAndSubmitDisabled() {
        val vm = OtpViewModel()
        assertEquals(List(6) { "" }, vm.current.digits)
        assertEquals(0, vm.current.focusedIndex)
        assertFalse(vm.current.isComplete)
    }

    @Test
    fun typingADigitAdvancesFocus() {
        val vm = OtpViewModel()
        vm.onBoxTextChange(0, "4")
        assertEquals("4", vm.current.digits[0])
        assertEquals(1, vm.current.focusedIndex)
    }

    @Test
    fun nonDigitsAreRejected() {
        val vm = OtpViewModel()
        vm.onBoxTextChange(0, "a")
        vm.onBoxTextChange(0, ".")
        assertEquals("", vm.current.digits[0])
        assertEquals(0, vm.current.focusedIndex)
    }

    @Test
    fun typingIntoAFilledBoxReplacesTheDigit() {
        val vm = OtpViewModel()
        vm.onBoxTextChange(0, "4")
        vm.onBoxFocused(0)
        vm.onBoxTextChange(0, "47")
        assertEquals("7", vm.current.digits[0])
        assertEquals("", vm.current.digits[1])
    }

    @Test
    fun fastTypingBeforeFocusMovesGoesToTheNextBox() {
        val vm = OtpViewModel()
        vm.onBoxTextChange(0, "2")
        vm.onBoxTextChange(0, "21")
        assertEquals(listOf("2", "1", "", "", "", ""), vm.current.digits)
        assertEquals(2, vm.current.focusedIndex)
    }

    @Test
    fun editsReturnTheBoxToFocusNext() {
        val vm = OtpViewModel()
        assertEquals(1, vm.onBoxTextChange(0, "7"))
        assertEquals(0, vm.onBackspaceOnEmptyBox(1))
        assertEquals(5, vm.onBoxTextChange(0, "Your code is 482913"))
    }

    @Test
    fun deletingADigitClearsOnlyThatBox() {
        val vm = OtpViewModel()
        vm.type("12")
        vm.onBoxTextChange(1, "")
        assertEquals(listOf("1", "", "", "", "", ""), vm.current.digits)
    }

    @Test
    fun backspaceOnEmptyBoxClearsPreviousAndMovesBack() {
        val vm = OtpViewModel()
        vm.type("123")
        vm.onBackspaceOnEmptyBox(3)
        assertEquals(listOf("1", "2", "", "", "", ""), vm.current.digits)
        assertEquals(2, vm.current.focusedIndex)
    }

    @Test
    fun backspaceOnFirstEmptyBoxDoesNothing() {
        val vm = OtpViewModel()
        vm.onBackspaceOnEmptyBox(0)
        assertEquals(0, vm.current.focusedIndex)
    }

    @Test
    fun pastingTextExtractsTheDigits() {
        val vm = OtpViewModel()
        vm.onBoxTextChange(0, "Your code is 482913")
        assertEquals(listOf("4", "8", "2", "9", "1", "3"), vm.current.digits)
        assertTrue(vm.current.isComplete)
    }

    @Test
    fun pastingAFullCodeFillsFromTheFirstBoxWhateverBoxIsFocused() {
        val vm = OtpViewModel()
        vm.onBoxTextChange(3, "Your code is 482913")
        assertEquals("482913", vm.current.code)
    }

    @Test
    fun pastingIntoAFilledBoxIgnoresTheExistingDigit() {
        val vm = OtpViewModel()
        vm.onBoxTextChange(0, "9")
        vm.onBoxTextChange(0, "9Your code is 482913")
        assertEquals("482913", vm.current.code)
    }

    @Test
    fun pastingAPartialCodeFillsFromTheCurrentBox() {
        val vm = OtpViewModel()
        vm.onPaste(2, "a1b2")
        assertEquals(listOf("", "", "1", "2", "", ""), vm.current.digits)
        assertEquals(4, vm.current.focusedIndex)
    }

    @Test
    fun correctCodeShowsSuccess() {
        val vm = OtpViewModel()
        vm.type("123456")
        vm.onSubmit()
        assertEquals(OtpStatus.Success, vm.current.status)
    }

    @Test
    fun wrongCodeShowsErrorClearsBoxesAndRefocusesFirst() {
        val vm = OtpViewModel()
        vm.type("654321")
        vm.onSubmit()
        assertEquals(OtpStatus.Error, vm.current.status)
        assertEquals(List(6) { "" }, vm.current.digits)
        assertEquals(0, vm.current.focusedIndex)
    }

    @Test
    fun submitIsIgnoredUntilAllDigitsAreFilled() {
        val vm = OtpViewModel()
        vm.type("12345")
        vm.onSubmit()
        assertEquals(OtpStatus.None, vm.current.status)
        assertEquals("12345", vm.current.code)
    }

    @Test
    fun unchangedTextDoesNotClearTheErrorMessage() {
        val vm = OtpViewModel()
        vm.type("000000")
        vm.onSubmit()
        vm.onBoxTextChange(0, "")
        assertEquals(OtpStatus.Error, vm.current.status)
    }

    @Test
    fun typingAfterAnErrorClearsTheMessage() {
        val vm = OtpViewModel()
        vm.type("000000")
        vm.onSubmit()
        vm.onBoxTextChange(0, "1")
        assertEquals(OtpStatus.None, vm.current.status)
    }

    @Test
    fun resendCountsDownFromThirtyThenBecomesAvailable() = runTest(dispatcher) {
        val vm = OtpViewModel()
        runCurrent()
        assertEquals(30, vm.current.resendSecondsLeft)
        assertFalse(vm.current.canResend)

        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(20, vm.current.resendSecondsLeft)

        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(0, vm.current.resendSecondsLeft)
        assertTrue(vm.current.canResend)
    }

    @Test
    fun resendIsIgnoredDuringCountdown() = runTest(dispatcher) {
        val vm = OtpViewModel()
        advanceTimeBy(5_000)
        runCurrent()
        vm.onResend()
        assertEquals(25, vm.current.resendSecondsLeft)
    }

    @Test
    fun resendRestartsTheCountdown() = runTest(dispatcher) {
        val vm = OtpViewModel()
        advanceTimeBy(30_000)
        runCurrent()
        assertTrue(vm.current.canResend)

        vm.onResend()
        runCurrent()
        assertEquals(30, vm.current.resendSecondsLeft)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(29, vm.current.resendSecondsLeft)
    }

    @Test
    fun countdownIsFormattedAsMinutesAndSeconds() {
        assertEquals("0:30", formatCountdown(30))
        assertEquals("0:05", formatCountdown(5))
        assertEquals("0:00", formatCountdown(0))
    }
}
