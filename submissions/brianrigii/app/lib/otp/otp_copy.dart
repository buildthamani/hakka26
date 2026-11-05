/// User-facing copy for the OTP screen, exactly as listed in issue #1.
abstract final class OtpCopy {
  static const title = 'Enter your code';
  static const subtitle = 'A 6-digit code has been sent to your phone.';
  static const resendPrompt = "Didn't receive the code? ";
  static const resend = 'Resend';
  static const submit = 'Submit';
  static const success = "You're in.";
  static const error = "That code didn't work. Try again.";

  /// Shown in an empty box. Deliberately not a digit, so it never reads as `0`.
  static const placeholder = '–';

  /// `Resend in 0:SS` while the cooldown runs.
  static String resendIn(int secondsLeft) {
    final minutes = secondsLeft ~/ 60;
    final seconds = (secondsLeft % 60).toString().padLeft(2, '0');
    return 'Resend in $minutes:$seconds';
  }

  /// Screen-reader label for each box: `Digit N of 6`.
  static String digitLabel(int position, int length) =>
      'Digit $position of $length';
}
