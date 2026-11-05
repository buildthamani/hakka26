import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/otp/otp_controller.dart';

void main() {
  group('OtpController', () {
    late OtpController otp;

    setUp(() => otp = OtpController());
    tearDown(() => otp.dispose());

    test('typing a digit fills the box and moves focus forward', () {
      otp.input(0, '4');
      expect(otp.digits.first, '4');
      expect(otp.focusIndex, 1);
    });

    test('non-digits are rejected', () {
      otp.input(0, 'a');
      expect(otp.code, '');
    });

    test('pasting text extracts the six digits', () {
      otp.input(2, 'Your code is 482913');
      expect(otp.code, '482913');
      expect(otp.isComplete, isTrue);
    });

    test('backspace on an empty box clears the previous digit', () {
      otp.input(0, '1');
      otp.input(1, '2');
      expect(otp.backspaceOnEmpty(2), isTrue);
      expect(otp.digits[1], '');
      expect(otp.focusIndex, 1);
    });

    test('backspace on a filled box is left to the text field', () {
      otp.input(0, '1');
      expect(otp.backspaceOnEmpty(0), isFalse);
    });

    test('123456 succeeds', () {
      otp.input(0, '123456');
      otp.submit();
      expect(otp.status, OtpStatus.success);
    });

    test('a wrong code errors, clears and refocuses the first box', () {
      otp.input(0, '654321');
      otp.submit();
      expect(otp.status, OtpStatus.error);
      expect(otp.code, '');
      expect(otp.focusIndex, 0);
    });

    test('submit does nothing until all six digits are filled', () {
      otp.input(0, '123');
      otp.submit();
      expect(otp.status, OtpStatus.idle);
    });
  });
}
