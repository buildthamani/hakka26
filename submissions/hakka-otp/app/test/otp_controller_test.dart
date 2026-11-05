import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/otp/otp_code.dart';
import 'package:hakka/otp/otp_controller.dart';

void main() {
  late OtpController controller;

  setUp(() => controller = OtpController());
  tearDown(() => controller.dispose());

  test('submit is disabled until six digits are entered', () {
    controller.updateCode('12345');
    expect(controller.canSubmit, isFalse);
    controller.updateCode('123456');
    expect(controller.canSubmit, isTrue);
  });

  test('123456 succeeds', () {
    controller.updateCode('123456');
    controller.submit();
    expect(controller.status, OtpStatus.success);
    expect(controller.canSubmit, isFalse);
  });

  test('a wrong code fails and clears the boxes', () {
    controller.updateCode('654321');
    controller.submit();
    expect(controller.status, OtpStatus.error);
    expect(controller.code, isEmpty);
    expect(controller.activeIndex, 0);
  });

  test('typing after an error clears the error', () {
    controller.updateCode('000000');
    controller.submit();
    controller.updateCode('1');
    expect(controller.status, OtpStatus.idle);
  });

  test('the verifier decides, not a hard-coded result', () {
    final custom = OtpController(verifier: (code) => code == '999999');
    addTearDown(custom.dispose);
    custom.updateCode('123456');
    custom.submit();
    expect(custom.status, OtpStatus.error);
  });

  test('resend is locked for 30 seconds, then restarts the countdown', () {
    expect(controller.secondsLeft, kResendSeconds);
    expect(controller.canResend, isFalse);
    controller.resend();
    expect(controller.secondsLeft, kResendSeconds);

    for (var i = 0; i < kResendSeconds; i++) {
      controller.tick();
    }
    expect(controller.canResend, isTrue);

    controller.resend();
    expect(controller.secondsLeft, kResendSeconds);
    expect(controller.canResend, isFalse);
  });
}
