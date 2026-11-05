import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';

enum OtpStatus { idle, success, error }

/// Owns every piece of OTP state: the digits, which box has focus, the result
/// of the last submit and the resend countdown. Widgets only render this state
/// and forward user input to it.
class OtpController extends ChangeNotifier {
  OtpController({this.expectedCode = demoCode}) {
    _startResendCountdown();
  }

  static const codeLength = 6;
  static const demoCode = '123456';
  static const resendCooldownSeconds = 30;

  static final _nonDigit = RegExp(r'\D');

  /// The code [submit] accepts. There is no backend, so it is checked locally.
  final String expectedCode;

  /// Seconds until Resend unlocks; 0 means it is available.
  ///
  /// Kept apart from [notifyListeners] so a tick redraws only the resend line
  /// and never counts as a state change that could move keyboard focus.
  final ValueNotifier<int> resendSecondsLeft = ValueNotifier(0);

  final List<String> _digits = List.filled(codeLength, '');
  int _focusedIndex = 0;
  OtpStatus _status = OtpStatus.idle;
  Timer? _resendTimer;

  /// One entry per box; an empty string means the box is empty.
  List<String> get digits => List.unmodifiable(_digits);
  int get focusedIndex => _focusedIndex;
  OtpStatus get status => _status;
  bool get isComplete => _digits.every((digit) => digit.isNotEmpty);
  bool get canResend => resendSecondsLeft.value == 0;

  /// Applies a keystroke or pasted text entered at box [index].
  ///
  /// Only the digits in [text] are kept, so non-digits are rejected and
  /// `Your code is 482913` yields `482913`. A full code fills every box wherever
  /// it was pasted; anything shorter fills forward from [index]. Focus then moves
  /// to the box after the last digit written.
  void enter(int index, String text) {
    final entered = text.replaceAll(_nonDigit, '');
    if (entered.isEmpty) return;

    final start = entered.length >= codeLength ? 0 : index;
    final count = math.min(entered.length, codeLength - start);
    for (var offset = 0; offset < count; offset++) {
      _digits[start + offset] = entered[offset];
    }
    _focusedIndex = math.min(start + count, codeLength - 1);
    _status = OtpStatus.idle;
    notifyListeners();
  }

  /// Clears box [index], or the previous box when [index] is already empty.
  void backspace(int index) {
    if (_digits[index].isNotEmpty) {
      _digits[index] = '';
    } else if (index > 0) {
      _digits[index - 1] = '';
      _focusedIndex = index - 1;
    } else {
      return;
    }
    _status = OtpStatus.idle;
    notifyListeners();
  }

  /// Records that the user moved focus to box [index] themselves.
  void focusBox(int index) {
    if (index == _focusedIndex) return;
    _focusedIndex = index;
    notifyListeners();
  }

  void submit() {
    if (!isComplete) return;

    if (_digits.join() == expectedCode) {
      _status = OtpStatus.success;
    } else {
      _status = OtpStatus.error;
      _digits.fillRange(0, codeLength, '');
      _focusedIndex = 0;
    }
    notifyListeners();
  }

  void resend() {
    if (canResend) _startResendCountdown();
  }

  void _startResendCountdown() {
    resendSecondsLeft.value = resendCooldownSeconds;
    _resendTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      resendSecondsLeft.value--;
      if (canResend) timer.cancel();
    });
  }

  @override
  void dispose() {
    _resendTimer?.cancel();
    resendSecondsLeft.dispose();
    super.dispose();
  }
}
