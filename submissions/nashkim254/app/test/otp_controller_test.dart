import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/otp/otp_controller.dart';

void main() {
  late OtpController otp;

  setUp(() => otp = OtpController());
  tearDown(() => otp.dispose());

  void type(String digits) {
    for (var i = 0; i < digits.length; i++) {
      otp.input(otp.activeIndex, digits[i]);
    }
  }

  test('typing a digit advances to the next box', () {
    otp.input(0, '4');
    expect(otp.digits.first, '4');
    expect(otp.activeIndex, 1);
  });

  test('non-digits are ignored', () {
    otp.input(0, 'a');
    expect(otp.digits.first, '');
    expect(otp.activeIndex, 0);
  });

  test('pasting text extracts the six digits', () {
    otp.paste('Your code is 482913');
    expect(otp.code, '482913');
    expect(otp.isComplete, isTrue);
  });

  test('pasting a full code into a later box fills from the first box', () {
    otp.input(3, 'Your code is 482913');
    expect(otp.code, '482913');
  });

  test('typing over an existing digit replaces it', () {
    otp.input(0, '4');
    otp.input(0, '47');
    expect(otp.digits.first, '7');
  });

  test(
    'backspace on an empty box clears the previous digit and moves back',
    () {
      type('12');
      expect(otp.activeIndex, 2);
      otp.backspace(2);
      expect(otp.digits, ['1', '', '', '', '', '']);
      expect(otp.activeIndex, 1);
    },
  );

  test('backspace on the first empty box does nothing', () {
    otp.backspace(0);
    expect(otp.activeIndex, 0);
    expect(otp.code, '');
  });

  test('submit does nothing until all six digits are filled', () {
    type('12345');
    otp.submit();
    expect(otp.status, OtpStatus.idle);
  });

  test('123456 succeeds', () {
    type('123456');
    otp.submit();
    expect(otp.status, OtpStatus.success);
  });

  test(
    'a wrong code shows an error, clears the boxes and refocuses the first',
    () {
      type('654321');
      final before = otp.focusRequest;
      otp.submit();
      expect(otp.status, OtpStatus.error);
      expect(otp.code, '');
      expect(otp.activeIndex, 0);
      expect(otp.focusRequest, greaterThan(before));
    },
  );

  test('uses the injected verifier', () {
    otp.dispose();
    otp = OtpController(verifier: (code) => code == '000000');
    type('000000');
    otp.submit();
    expect(otp.status, OtpStatus.success);
  });

  test('resend is locked for 30 seconds at the start', () {
    expect(otp.secondsLeft, 30);
    expect(otp.canResend, isFalse);
  });
}
