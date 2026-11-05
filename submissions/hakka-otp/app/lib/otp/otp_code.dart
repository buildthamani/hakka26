/// Pure rules for the one-time code. No Flutter imports, so they are easy to test.
library;

const int kOtpLength = 6;
const int kResendSeconds = 30;

final RegExp _nonDigits = RegExp(r'\D');

String digitsOnly(String text) => text.replaceAll(_nonDigits, '');

/// Works out the new code after the hidden input changes from [previous] to [incoming].
///
/// - Non-digits are always dropped.
/// - Deleting text (including backspace on an empty box) keeps the remaining digits.
/// - An insert with a full code in it, such as pasting `Your code is 482913`, replaces the code.
/// - Otherwise the inserted digits are appended, and anything past six digits is ignored.
String nextCode(String previous, String incoming) {
  if (incoming.length < previous.length) {
    return _cap(digitsOnly(incoming));
  }
  final inserted = incoming.startsWith(previous) ? incoming.substring(previous.length) : incoming;
  final insertedDigits = digitsOnly(inserted);
  if (insertedDigits.length >= kOtpLength) {
    return insertedDigits.substring(0, kOtpLength);
  }
  return _cap(digitsOnly(previous) + insertedDigits);
}

String _cap(String digits) => digits.length > kOtpLength ? digits.substring(0, kOtpLength) : digits;

/// Formats remaining seconds as `0:SS`, for example `0:07`.
String formatCountdown(int seconds) {
  final minutes = seconds ~/ 60;
  final rest = (seconds % 60).toString().padLeft(2, '0');
  return '$minutes:$rest';
}
