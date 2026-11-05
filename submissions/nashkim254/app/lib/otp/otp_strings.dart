/// Copy shown on the OTP screen.
abstract final class OtpStrings {
  static const title = 'Enter your code';
  static const subtitle = 'A 6-digit code has been sent to your phone.';
  static const resendPrompt = "Didn't receive the code? ";
  static const resend = 'Resend';
  static const submit = 'Submit';
  static const success = "You're in.";
  static const error = "That code didn't work. Try again.";
  static const placeholder = '–';

  static String resendIn(int seconds) =>
      'Resend in ${seconds ~/ 60}:${(seconds % 60).toString().padLeft(2, '0')}';

  static String digitLabel(int position, int total) =>
      'Digit $position of $total';
}

/// Test IDs, also exposed as semantics identifiers.
abstract final class OtpTestIds {
  static String digit(int position) => 'otp_digit_$position';
  static const submit = 'otp_submit';
  static const resend = 'otp_resend';
  static const status = 'otp_status';
}
