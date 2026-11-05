import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/otp/otp_controller.dart';

void main() {
  late OtpController otp;

  setUp(() => otp = OtpController());
  tearDown(() => otp.dispose());

  void typeCode(String code) {
    for (var index = 0; index < code.length; index++) {
      otp.enter(index, code[index]);
    }
  }

  group('entering digits', () {
    test('a keystroke fills the box and moves focus to the next one', () {
      otp.enter(0, '7');

      expect(otp.digits, ['7', '', '', '', '', '']);
      expect(otp.focusedIndex, 1);
    });

    test('non-digits are rejected', () {
      var notified = false;
      otp.addListener(() => notified = true);

      otp.enter(0, 'a');

      expect(otp.digits, everyElement(isEmpty));
      expect(otp.focusedIndex, 0);
      expect(notified, isFalse);
    });

    test('typing in a filled box replaces its digit', () {
      otp.enter(0, '7');
      otp.focusBox(0);

      otp.enter(0, '8');

      expect(otp.digits.first, '8');
      expect(otp.focusedIndex, 1);
    });

    test('focus stays on the last box once it is filled', () {
      typeCode('123456');

      expect(otp.focusedIndex, OtpController.codeLength - 1);
    });
  });

  group('pasting', () {
    test('extracts the digits from surrounding text', () {
      otp.enter(0, 'Your code is 482913');

      expect(otp.digits.join(), '482913');
    });

    test('a full code fills every box wherever it is pasted', () {
      otp.enter(3, 'Your code is 482913');

      expect(otp.digits.join(), '482913');
      expect(otp.focusedIndex, OtpController.codeLength - 1);
    });

    test('keeps only the first six digits of a longer run', () {
      otp.enter(0, '48291377');

      expect(otp.digits.join(), '482913');
    });

    test('a partial code fills forward from the box it was pasted in', () {
      otp.enter(1, '48');

      expect(otp.digits, ['', '4', '8', '', '', '']);
      expect(otp.focusedIndex, 3);
    });

    test('a partial code never writes past the last box', () {
      otp.enter(4, '482');

      expect(otp.digits, ['', '', '', '', '4', '8']);
      expect(otp.focusedIndex, OtpController.codeLength - 1);
    });
  });

  group('backspace', () {
    test('on a filled box clears it and keeps focus there', () {
      typeCode('12');
      otp.focusBox(1);

      otp.backspace(1);

      expect(otp.digits, ['1', '', '', '', '', '']);
      expect(otp.focusedIndex, 1);
    });

    test('on an empty box moves back and clears the previous digit', () {
      typeCode('12');

      otp.backspace(2);

      expect(otp.digits, ['1', '', '', '', '', '']);
      expect(otp.focusedIndex, 1);
    });

    test('on the empty first box does nothing', () {
      var notified = false;
      otp.addListener(() => notified = true);

      otp.backspace(0);

      expect(otp.focusedIndex, 0);
      expect(notified, isFalse);
    });
  });

  group('submitting', () {
    test('is complete only once all six boxes are filled', () {
      typeCode('12345');
      expect(otp.isComplete, isFalse);

      otp.enter(5, '6');
      expect(otp.isComplete, isTrue);
    });

    test('an incomplete code is ignored', () {
      typeCode('12345');

      otp.submit();

      expect(otp.status, OtpStatus.idle);
      expect(otp.digits.join(), '12345');
    });

    test('the expected code succeeds and keeps the digits', () {
      typeCode('123456');

      otp.submit();

      expect(otp.status, OtpStatus.success);
      expect(otp.digits.join(), '123456');
    });

    test('checks against the configured code, not a fixed value', () {
      final custom = OtpController(expectedCode: '654321');
      addTearDown(custom.dispose);
      custom.enter(0, '654321');

      custom.submit();

      expect(custom.status, OtpStatus.success);
    });

    test('any other code errors, clears every box and refocuses the first', () {
      typeCode('123457');

      otp.submit();

      expect(otp.status, OtpStatus.error);
      expect(otp.digits, everyElement(isEmpty));
      expect(otp.focusedIndex, 0);
    });

    test('typing after an error clears the error', () {
      typeCode('000000');
      otp.submit();

      otp.enter(0, '1');

      expect(otp.status, OtpStatus.idle);
    });

    test('deleting after a success clears the success', () {
      typeCode('123456');
      otp.submit();

      otp.backspace(5);

      expect(otp.status, OtpStatus.idle);
    });
  });

  group('resend countdown', () {
    // testWidgets supplies the fake clock that tester.pump advances, so the
    // controller is built inside that zone. It must also be disposed inside the
    // test body: the pending-timer check runs before addTearDown callbacks.
    void testCountdown(
      String description,
      Future<void> Function(WidgetTester tester, OtpController otp) body,
    ) {
      testWidgets(description, (tester) async {
        final controller = OtpController();
        await body(tester, controller);
        controller.dispose();
      });
    }

    testCountdown('starts locked at 30 seconds and ticks once a second', (
      tester,
      otp,
    ) async {
      expect(otp.resendSecondsLeft.value, 30);
      expect(otp.canResend, isFalse);

      await tester.pump(const Duration(seconds: 1));

      expect(otp.resendSecondsLeft.value, 29);
    });

    testCountdown('unlocks after exactly 30 seconds and stays at zero', (
      tester,
      otp,
    ) async {
      await tester.pump(const Duration(seconds: 29));
      expect(otp.canResend, isFalse);

      await tester.pump(const Duration(seconds: 1));
      expect(otp.canResend, isTrue);

      await tester.pump(const Duration(seconds: 5));
      expect(otp.resendSecondsLeft.value, 0);
    });

    testCountdown('resend is ignored while locked', (tester, otp) async {
      await tester.pump(const Duration(seconds: 10));

      otp.resend();

      expect(otp.resendSecondsLeft.value, 20);
    });

    testCountdown('resend restarts the countdown once unlocked', (
      tester,
      otp,
    ) async {
      await tester.pump(const Duration(seconds: 30));

      otp.resend();

      expect(otp.resendSecondsLeft.value, 30);
      await tester.pump(const Duration(seconds: 1));
      expect(otp.resendSecondsLeft.value, 29);
    });

    testCountdown('a tick does not notify the main listeners', (
      tester,
      otp,
    ) async {
      var notified = false;
      otp.addListener(() => notified = true);

      await tester.pump(const Duration(seconds: 3));

      expect(notified, isFalse);
    });
  });
}
