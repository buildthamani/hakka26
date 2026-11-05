import 'dart:math' as math;
import 'package:flutter/foundation.dart';

/// Digits and the editing position have one source of truth.
class OtpController extends ChangeNotifier {
  static const length = 6;
  final List<String> _digits = List.filled(length, '');
  int _activeIndex = 0;
  int get activeIndex => _activeIndex;
  String digitAt(int index) => _digits[index];
  String get code => _digits.join();
  bool get isComplete => _digits.every((digit) => digit.isNotEmpty);

  void select(int index) {
    assert(index >= 0 && index < length);
    if (_activeIndex == index) return;
    _activeIndex = index;
    notifyListeners();
  }

  void enter(int index, String text) {
    final digits = text.replaceAll(RegExp(r'[^0-9]'), '');
    if (digits.isEmpty) return;
    // A full pasted code replaces the OTP even when a later box is focused.
    final start = digits.length >= length ? 0 : index;
    final count = math.min(digits.length, length - start);
    for (var offset = 0; offset < count; offset++) {
      _digits[start + offset] = digits[offset];
    }
    _activeIndex = math.min(start + count, length - 1);
    notifyListeners();
  }

  void backspace(int index) {
    if (_digits[index].isEmpty && index > 0) index--;
    _digits[index] = '';
    _activeIndex = index;
    notifyListeners();
  }

  void clear() {
    _digits.fillRange(0, length, '');
    _activeIndex = 0;
    notifyListeners();
  }
}
