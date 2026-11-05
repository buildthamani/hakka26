package ke.droidcon.hakka
import org.junit.Assert.*
import org.junit.Test
class OtpStateTest {
 @Test fun initialDisabled() { val s = OtpState(); assertFalse(s.complete); assertEquals(s, s.submit()) }
 @Test fun advances() { val s = OtpState().input(0,"4"); assertEquals("4",s.digits[0]); assertEquals(1,s.focus) }
 @Test fun paste() { val s=OtpState().input(3,"Your code is 482913"); assertEquals("482913",s.digits.joinToString("")); assertTrue(s.complete); assertEquals(5,s.focus) }
 @Test fun digitsOnly() { assertEquals(OtpState(),OtpState().input(0,"abc!")); assertEquals("123456",OtpState().input(0,"12345678").digits.joinToString("")) }
 @Test fun emptyBackspace() { val s=OtpState().input(0,"4").backspace(1); assertEquals("",s.digits[0]); assertEquals(0,s.focus); assertEquals(0,OtpState().backspace(0).focus) }
 @Test fun filledBackspace() { val s=OtpState().input(0,"12").backspace(1); assertEquals("1",s.digits[0]); assertEquals("",s.digits[1]); assertEquals(1,s.focus) }
 @Test fun success() { assertEquals("You're in.",OtpState().input(0,"123456").submit().status) }
 @Test fun failureResets() { val s=OtpState().input(0,"482913").submit(); assertEquals("That code didn't work. Try again.",s.status); assertEquals(List(6){""},s.digits); assertEquals(0,s.focus); assertFalse(s.complete); assertEquals("",s.input(0,"1").status) }
 @Test fun countdown() { assertEquals(30,remainingSeconds(30000,0)); assertEquals(1,remainingSeconds(30000,29999)); assertEquals(0,remainingSeconds(30000,30000)); assertEquals(0,remainingSeconds(30000,40000)); assertEquals(30,remainingSeconds(70000,40000)) }
}
