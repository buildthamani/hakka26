import 'dart:async';

import 'package:flutter/foundation.dart';

/// The code the demo backend accepts.
const demoValidCode = '123456';

/// Checks a full code. Swap this for a real API call in production.
typedef OtpVerifier = bool Function(String code);

bool verifyDemoCode(String code) => code == demoValidCode;

enum OtpStatus { idle, success, error }

final _nonDigits = RegExp(r'\D');

/// Holds all state for the OTP screen: the digits, which box is active,
/// the verification result and the resend countdown.
class OtpController extends ChangeNotifier {
  OtpController({
    this.length = 6,
    this.resendCooldown = const Duration(seconds: 30),
    OtpVerifier verifier = verifyDemoCode,
  }) : _verifier = verifier,
       _digits = List.filled(length, '') {
    _startCountdown();
  }

  final int length;
  final Duration resendCooldown;
  final OtpVerifier _verifier;

  final List<String> _digits;
  int _activeIndex = 0;
  int _focusRequest = 0;
  OtpStatus _status = OtpStatus.idle;
  int _secondsLeft = 0;
  Timer? _timer;

  List<String> get digits => List.unmodifiable(_digits);
  String get code => _digits.join();
  bool get isComplete => _digits.every((d) => d.isNotEmpty);

  /// The box that should hold focus.
  int get activeIndex => _activeIndex;

  /// Bumped whenever the controller wants the UI to move focus to
  /// [activeIndex], so the UI only moves focus when asked to.
  int get focusRequest => _focusRequest;

  OtpStatus get status => _status;
  int get secondsLeft => _secondsLeft;
  bool get canResend => _secondsLeft == 0;

  /// Handles new text in box [index], whether typed or pasted.
  void input(int index, String text) {
    final entered = text.replaceAll(_nonDigits, '');
    if (entered.isEmpty) {
      _digits[index] = '';
      _resetStatus();
      notifyListeners();
      return;
    }
    if (entered.length == 1) {
      _setDigitAndAdvance(index, entered);
      return;
    }
    // Typed over an existing digit: keep the new one.
    final previous = _digits[index];
    if (entered.length == 2 &&
        previous.isNotEmpty &&
        entered.contains(previous)) {
      _setDigitAndAdvance(
        index,
        entered[0] == previous ? entered[1] : entered[0],
      );
      return;
    }
    paste(entered, from: index);
  }

  /// Fills the boxes from pasted text such as "Your code is 482913".
  /// A full code always fills from the first box.
  void paste(String text, {int from = 0}) {
    final pasted = text.replaceAll(_nonDigits, '');
    if (pasted.isEmpty) return;
    final start = pasted.length >= length ? 0 : from;
    final count = (length - start).clamp(0, pasted.length);
    for (var i = 0; i < count; i++) {
      _digits[start + i] = pasted[i];
    }
    _resetStatus();
    _moveFocus((start + count).clamp(0, length - 1));
  }

  /// Backspace on an empty box moves back and clears the previous digit.
  void backspace(int index) {
    if (_digits[index].isNotEmpty) {
      _digits[index] = '';
      _resetStatus();
      notifyListeners();
      return;
    }
    if (index == 0) return;
    _digits[index - 1] = '';
    _resetStatus();
    _moveFocus(index - 1);
  }

  /// Records that the user focused [index] themselves (e.g. by tapping it).
  void activate(int index) {
    if (_activeIndex == index) return;
    _activeIndex = index;
    notifyListeners();
  }

  /// Verifies the code. A wrong code clears every box and refocuses the first.
  void submit() {
    if (!isComplete) return;
    if (_verifier(code)) {
      _status = OtpStatus.success;
      notifyListeners();
      return;
    }
    _status = OtpStatus.error;
    _digits.fillRange(0, length, '');
    _moveFocus(0);
  }

  void resend() {
    if (!canResend) return;
    _resetStatus();
    _startCountdown();
  }

  void _setDigitAndAdvance(int index, String digit) {
    _digits[index] = digit;
    _resetStatus();
    _moveFocus(index < length - 1 ? index + 1 : index);
  }

  void _moveFocus(int index) {
    _activeIndex = index;
    _focusRequest++;
    notifyListeners();
  }

  /// Any edit hides the previous result message.
  void _resetStatus() => _status = OtpStatus.idle;

  void _startCountdown() {
    _timer?.cancel();
    _secondsLeft = resendCooldown.inSeconds;
    notifyListeners();
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      _secondsLeft--;
      if (_secondsLeft <= 0) {
        _secondsLeft = 0;
        timer.cancel();
      }
      notifyListeners();
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}
