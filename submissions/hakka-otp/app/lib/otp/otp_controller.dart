import 'dart:async';

import 'package:flutter/foundation.dart';

import 'otp_code.dart';

enum OtpStatus { idle, success, error }

typedef OtpVerifier = bool Function(String code);

/// Demo verifier for the challenge: only `123456` is accepted.
bool demoVerifier(String code) => code == '123456';

/// Holds all OTP screen state in one place: the code, the result and the resend countdown.
class OtpController extends ChangeNotifier {
  OtpController({this.verifier = demoVerifier}) {
    _startCountdown();
  }

  final OtpVerifier verifier;

  String _code = '';
  OtpStatus _status = OtpStatus.idle;
  int _secondsLeft = 0;
  Timer? _timer;

  String get code => _code;
  OtpStatus get status => _status;
  int get secondsLeft => _secondsLeft;
  bool get canSubmit => _code.length == kOtpLength && _status != OtpStatus.success;
  bool get canResend => _secondsLeft == 0;

  /// The box that has focus while the input is focused: the first empty one.
  int get activeIndex => _code.length.clamp(0, kOtpLength - 1);

  void updateCode(String value) {
    final next = digitsOnly(value);
    final capped = next.length > kOtpLength ? next.substring(0, kOtpLength) : next;
    if (capped == _code) return;
    _code = capped;
    if (_status == OtpStatus.error) _status = OtpStatus.idle;
    notifyListeners();
  }

  /// Checks the code. On failure the boxes are cleared; the screen moves focus back to box 1.
  void submit() {
    if (!canSubmit) return;
    if (verifier(_code)) {
      _status = OtpStatus.success;
    } else {
      _status = OtpStatus.error;
      _code = '';
    }
    notifyListeners();
  }

  void resend() {
    if (!canResend) return;
    _startCountdown();
  }

  void _startCountdown() {
    _timer?.cancel();
    _secondsLeft = kResendSeconds;
    _timer = Timer.periodic(const Duration(seconds: 1), (_) => tick());
    notifyListeners();
  }

  @visibleForTesting
  void tick() {
    if (_secondsLeft == 0) return;
    _secondsLeft--;
    if (_secondsLeft == 0) _timer?.cancel();
    notifyListeners();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}
