import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/foundation.dart';

enum OtpStatus { idle, success, error }

/// Single source of truth for the OTP screen: digits, focus requests,
/// verification result and the resend countdown.
class OtpController extends ChangeNotifier {
  OtpController({
    this.length = 6,
    this.resendCooldown = const Duration(seconds: 30),
    bool Function(String code)? verifier,
  })  : _verify = verifier ?? _defaultVerifier,
        _digits = List.filled(length, '') {
    _startCountdown();
  }

  static const validDemoCode = '123456';
  static bool _defaultVerifier(String code) => code == validDemoCode;

  final int length;
  final Duration resendCooldown;
  final bool Function(String code) _verify;

  final List<String> _digits;
  OtpStatus _status = OtpStatus.idle;
  int _focusIndex = 0;
  int _focusRequest = 0;
  int _secondsLeft = 0;
  Timer? _timer;

  List<String> get digits => List.unmodifiable(_digits);
  String get code => _digits.join();
  bool get isComplete => _digits.every((d) => d.isNotEmpty);
  OtpStatus get status => _status;
  int get secondsLeft => _secondsLeft;
  bool get canResend => _secondsLeft == 0;

  /// Index of the box that should hold focus, and a counter that changes
  /// every time focus is (re)requested, so the view can react even when the
  /// index itself is unchanged.
  int get focusIndex => _focusIndex;
  int get focusRequest => _focusRequest;

  /// Handles text entered into box [index]: a single digit, an empty string
  /// (deleted), or a longer pasted string whose digits are spread across the
  /// boxes. Non-digits are dropped.
  void input(int index, String text) {
    final incoming = text.replaceAll(RegExp(r'\D'), '');
    _status = OtpStatus.idle;

    if (incoming.isEmpty) {
      _digits[index] = '';
    } else if (incoming.length == 1) {
      _digits[index] = incoming;
      _requestFocus(math.min(index + 1, length - 1));
    } else {
      // Paste: a full code fills from the first box, a partial one from here.
      final start = incoming.length >= length ? 0 : index;
      final chars = incoming.substring(0, math.min(incoming.length, length - start));
      for (var i = 0; i < chars.length; i++) {
        _digits[start + i] = chars[i];
      }
      _requestFocus(math.min(start + chars.length, length - 1));
    }
    notifyListeners();
  }

  /// Backspace pressed on box [index]. Returns true if it was handled, i.e.
  /// the box was empty and focus moved back, clearing the previous digit.
  bool backspaceOnEmpty(int index) {
    if (_digits[index].isNotEmpty || index == 0) return false;
    _digits[index - 1] = '';
    _status = OtpStatus.idle;
    _requestFocus(index - 1);
    notifyListeners();
    return true;
  }

  void focusChanged(int index) => _focusIndex = index;

  void submit() {
    if (!isComplete) return;
    if (_verify(code)) {
      _status = OtpStatus.success;
    } else {
      _status = OtpStatus.error;
      _digits.fillRange(0, length, '');
      _requestFocus(0);
    }
    notifyListeners();
  }

  void resend() {
    if (!canResend) return;
    _startCountdown();
    notifyListeners();
  }

  void _requestFocus(int index) {
    _focusIndex = index;
    _focusRequest++;
  }

  void _startCountdown() {
    _timer?.cancel();
    _secondsLeft = resendCooldown.inSeconds;
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
