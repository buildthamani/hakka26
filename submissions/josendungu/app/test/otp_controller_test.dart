import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/otp/otp_controller.dart';
import 'package:hakka/otp/otp_verifier.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late OtpController controller;
  setUp(
    () => controller = OtpController(
      verifier: const FixedCodeOtpVerifier('123456'),
    ),
  );
  tearDown(() => controller.dispose());

  test('extracts digits from pasted text', () {
    controller.onBoxChanged(0, 'Your code is 482913');
    expect(controller.code, '482913');
    expect(controller.isComplete, isTrue);
  });

  test('typing into a filled box replaces its digit', () {
    controller.onBoxChanged(0, '5');
    controller.onBoxChanged(0, '57');
    expect(controller.digits.first, '7');
  });

  test('verifies the code through the verifier', () async {
    controller.onBoxChanged(0, '123456');
    await controller.submit();
    expect(controller.status, OtpStatus.success);
  });

  test('a wrong code clears the digits', () async {
    controller.onBoxChanged(0, '111111');
    await controller.submit();
    expect(controller.status, OtpStatus.error);
    expect(controller.code, isEmpty);
  });

  test('starts with a 30 s resend cooldown', () {
    expect(controller.resendSecondsLeft, 30);
    expect(controller.canResend, isFalse);
  });
}
