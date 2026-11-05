package ke.droidcon.hakka.otp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OtpReducerTest {

    private lateinit var reducer: OtpReducer

    @Before
    fun setup() {
        reducer = OtpReducer()
    }

    @Test
    fun test_typing_digit_updates_state_and_advances_index() {
        reducer.onDigitsChanged("1")
        assertEquals("1", reducer.state.value.digits)
        assertEquals(1, reducer.state.value.activeIndex)

        reducer.onDigitsChanged("12")
        assertEquals("12", reducer.state.value.digits)
        assertEquals(2, reducer.state.value.activeIndex)
    }

    @Test
    fun test_backspace_clears_digit_and_moves_index_back() {
        reducer.onDigitsChanged("123")
        reducer.onDigitsChanged("12")
        assertEquals("12", reducer.state.value.digits)
        assertEquals(2, reducer.state.value.activeIndex)
    }

    @Test
    fun test_paste_strips_letters_and_truncates_to_six() {
        reducer.onDigitsChanged("Your code is 482913")
        assertEquals("482913", reducer.state.value.digits)
        assertEquals(5, reducer.state.value.activeIndex)
    }

    @Test
    fun test_submit_enabled_only_when_six_digits_entered() {
        reducer.onDigitsChanged("12345")
        assertFalse(reducer.state.value.isSubmitEnabled)

        reducer.onDigitsChanged("123456")
        assertTrue(reducer.state.value.isSubmitEnabled)
    }

    @Test
    fun test_timer_ticks_down_to_zero() {
        assertEquals(30, reducer.state.value.secondsLeft)

        reducer.onTick()
        assertEquals(29, reducer.state.value.secondsLeft)

        repeat(29) { reducer.onTick() }
        assertEquals(0, reducer.state.value.secondsLeft)

        // Should not drop below 0
        reducer.onTick()
        assertEquals(0, reducer.state.value.secondsLeft)
    }

    @Test
    fun test_resend_is_enabled_only_when_timer_is_zero() {
        assertFalse(reducer.state.value.canResend)
        repeat(30) { reducer.onTick() }
        assertTrue(reducer.state.value.canResend)
    }

    @Test
    fun test_resend_action_resets_timer_and_clears_digits() {
        reducer.onDigitsChanged("123456")
        repeat(30) { reducer.onTick() }

        reducer.onResend()

        assertEquals("", reducer.state.value.digits)
        assertEquals(30, reducer.state.value.secondsLeft)
        assertFalse(reducer.state.value.canResend)
        assertEquals(OtpStatus.Idle, reducer.state.value.status)
    }

    @Test
    fun test_submit_success_with_123456() {
        reducer.onDigitsChanged("123456")
        reducer.onSubmit()
        
        assertEquals(OtpStatus.Success, reducer.state.value.status)
        assertEquals("123456", reducer.state.value.digits)
    }

    @Test
    fun test_submit_error_with_other_digits() {
        reducer.onDigitsChanged("654321")
        reducer.onSubmit()
        
        assertEquals(OtpStatus.Error, reducer.state.value.status)
        assertEquals("", reducer.state.value.digits) // verify digits get cleared for retry
    }
}
