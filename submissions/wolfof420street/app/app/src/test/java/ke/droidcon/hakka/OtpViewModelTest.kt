package ke.droidcon.hakka

import ke.droidcon.hakka.domain.model.OtpState
import ke.droidcon.hakka.domain.model.OtpStatus
import ke.droidcon.hakka.domain.model.RESEND_SECONDS
import ke.droidcon.hakka.presentation.OtpViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpViewModelTest {

    private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Test
    fun initialStateHasSixEmptyDigitsAndIdleStatus() {
        val viewModel = OtpViewModel(externalScope = testScope)
        val state = viewModel.uiState.value

        assertEquals(6, state.digits.size)
        assertTrue(state.digits.all { it.isEmpty() })
        assertEquals(0, state.focusedIndex)
        assertEquals(OtpStatus.Idle, state.status)
        assertFalse(state.isComplete)
    }

    @Test
    fun onDigitInputAdvancesFocus() {
        val viewModel = OtpViewModel(externalScope = testScope)
        viewModel.onDigitInput(0, "5")

        val state = viewModel.uiState.value
        assertEquals("5", state.digits[0])
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun onPasteExtractsAndFillsDigits() {
        val viewModel = OtpViewModel(externalScope = testScope)
        viewModel.onPaste("Your code is 482913")

        val state = viewModel.uiState.value
        assertEquals("482913", state.code)
        assertTrue(state.isComplete)
    }

    @Test
    fun onSubmitValidCodeSetsSuccessStatus() {
        val viewModel = OtpViewModel(externalScope = testScope)
        viewModel.onPaste("123456")
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(OtpStatus.Success, state.status)
    }

    @Test
    fun onSubmitInvalidCodeSetsErrorStatusAndClearsDigits() {
        val viewModel = OtpViewModel(externalScope = testScope)
        viewModel.onPaste("999999")
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(OtpStatus.Error, state.status)
        assertEquals("", state.code)
        assertEquals(0, state.focusedIndex)
    }

    @Test
    fun onSubmitWhenIncompleteDoesNothing() {
        val viewModel = OtpViewModel(externalScope = testScope)
        viewModel.onPaste("123")
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(OtpStatus.Idle, state.status)
    }

    @Test
    fun onBackspaceClearsCurrentOrPreviousDigit() {
        val viewModel = OtpViewModel(externalScope = testScope)
        viewModel.onDigitInput(0, "2")
        viewModel.onDigitInput(1, "1")
        assertEquals("21", viewModel.uiState.value.code)

        // Backspace on empty index 2 clears index 1
        viewModel.onBackspace(2)
        assertEquals("2", viewModel.uiState.value.code)
        assertEquals(1, viewModel.uiState.value.focusedIndex)
    }

    @Test
    fun onResendRestartsTimerWhenCanResend() {
        val expiredState = OtpState(resendSecondsLeft = 0)
        val viewModel = OtpViewModel(externalScope = testScope, initialState = expiredState)

        assertTrue(viewModel.uiState.value.canResend)
        viewModel.onResend()
        assertEquals(RESEND_SECONDS, viewModel.uiState.value.resendSecondsLeft)
    }
}
