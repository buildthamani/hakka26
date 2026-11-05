import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/otp/otp_code.dart';

void main() {
  group('nextCode', () {
    test('typing appends one digit at a time', () {
      expect(nextCode('', '4'), '4');
      expect(nextCode('4', '48'), '48');
    });

    test('rejects non-digits', () {
      expect(nextCode('12', '12a'), '12');
      expect(nextCode('', ' -.'), '');
    });

    test('backspace removes the previous digit', () {
      expect(nextCode('215', '21'), '21');
      expect(nextCode('2', ''), '');
    });

    test('pasting text with a code extracts the six digits', () {
      expect(nextCode('', 'Your code is 482913'), '482913');
    });

    test('a pasted full code replaces a partial one', () {
      expect(nextCode('21', '21Your code is 482913'), '482913');
    });

    test('ignores digits beyond six', () {
      expect(nextCode('123456', '1234567'), '123456');
      expect(nextCode('1234', '1234567'), '123456');
    });
  });

  test('formatCountdown pads seconds as 0:SS', () {
    expect(formatCountdown(30), '0:30');
    expect(formatCountdown(7), '0:07');
    expect(formatCountdown(0), '0:00');
  });
}
