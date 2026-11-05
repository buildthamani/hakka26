import 'dart:async';

import 'package:flutter/widgets.dart';

enum OtpStatus { idle, success, error }

/// Decides whether a complete code is valid.
typedef OtpVerifier = bool Function(String code);

/// Holds all OTP screen state in one place: the digits, which box has focus,
/// the submit result and the resend cooldown.
///
/// Each box is a one-character text field. An empty box is never truly empty:
/// it holds an invisible [fieldSentinel], so pressing backspace in it still
/// produces an edit (the sentinel disappears). That is how an empty-box
/// backspace is detected on soft keyboards, which send no key event for it.
class OtpController extends ChangeNotifier {
  OtpController({
    this.length = 6,
    OtpVerifier? verifier,
    this.resendCooldown = const Duration(seconds: 30),
  }) : _verifier = verifier ?? acceptsDemoCode,
       _digits = List.filled(length, ''),
       focusNodes = List.generate(
         length,
         (i) => FocusNode(debugLabel: 'otp_digit_${i + 1}'),
       ),
       fieldControllers = List.generate(
         length,
         (_) => TextEditingController.fromValue(_fieldValue('')),
       );

  /// Zero-width space kept in front of each box's digit (see class docs).
  static const fieldSentinel = '​';

  /// The code the demo accepts. Stands in for a call to a verification API.
  static bool acceptsDemoCode(String code) => code == '123456';

  final int length;
  final Duration resendCooldown;
  final OtpVerifier _verifier;
  final List<String> _digits;
  final List<FocusNode> focusNodes;
  final List<TextEditingController> fieldControllers;

  OtpStatus _status = OtpStatus.idle;
  int _secondsUntilResend = 0;
  Timer? _resendTimer;

  List<String> get digits => List.unmodifiable(_digits);
  String get code => _digits.join();
  bool get isComplete => _digits.every((d) => d.isNotEmpty);
  OtpStatus get status => _status;
  int get secondsUntilResend => _secondsUntilResend;
  bool get canResend => _secondsUntilResend == 0;

  /// Handles an edit made by the user in box [index]: typing, deleting or
  /// pasting. Non-digits never reach here (the field filters them out).
  void onFieldChanged(int index, String value) {
    final previous = _digits[index];
    final typed = value.replaceAll(RegExp(r'\D'), '');
    final sentinelDeleted = !value.contains(fieldSentinel);

    if (typed.isEmpty) {
      if (sentinelDeleted && previous.isEmpty) {
        _clearPrevious(index);
      } else {
        _digits[index] = '';
      }
    } else {
      final entered = _withoutExistingDigit(typed, previous);
      if (entered.length == 1) {
        _digits[index] = entered;
        _focus(index + 1);
      } else if (entered.length > 1) {
        _spread(entered, index);
      }
    }

    _status = OtpStatus.idle;
    _changed();
  }

  /// Puts the caret back after the digit when a box is tapped, so the next
  /// keystroke replaces the digit and backspace deletes it.
  void onFieldTapped(int index) => _syncField(index, force: true);

  /// Checks the code. On failure the boxes are cleared and the first focused.
  void submit() {
    if (!isComplete) return;
    if (_verifier(code)) {
      _status = OtpStatus.success;
      for (final node in focusNodes) {
        node.unfocus();
      }
    } else {
      _status = OtpStatus.error;
      _digits.fillRange(0, length, '');
      _focus(0);
    }
    _changed();
  }

  /// Starts (or restarts) the resend cooldown. Call when a code is sent.
  void startResendCooldown() {
    _resendTimer?.cancel();
    _secondsUntilResend = resendCooldown.inSeconds;
    _resendTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      _secondsUntilResend--;
      if (_secondsUntilResend <= 0) {
        _secondsUntilResend = 0;
        timer.cancel();
      }
      notifyListeners();
    });
    notifyListeners();
  }

  /// Sends a new code and restarts the cooldown. Ignored while it is running.
  void resend() {
    if (!canResend) return;
    startResendCooldown();
  }

  /// Backspace in an empty box: step back and clear that box.
  void _clearPrevious(int index) {
    if (index == 0) return;
    _digits[index - 1] = '';
    _focus(index - 1);
  }

  /// Fills boxes from a multi-digit entry (a paste). A full-length code always
  /// fills from the first box; a shorter run fills from [index] onwards.
  void _spread(String entered, int index) {
    final start = entered.length >= length ? 0 : index;
    final count = (length - start).clamp(0, entered.length);
    for (var i = 0; i < count; i++) {
      _digits[start + i] = entered[i];
    }
    _focus(start + count);
  }

  /// Typing into a filled box yields the old digit plus the new input; drop
  /// the old digit so only what the user entered remains.
  static String _withoutExistingDigit(String typed, String previous) {
    if (previous.isEmpty || typed.length < 2) return typed;
    final at = typed.indexOf(previous);
    return at == -1 ? typed : typed.replaceRange(at, at + 1, '');
  }

  void _focus(int index) =>
      focusNodes[index.clamp(0, length - 1)].requestFocus();

  void _changed() {
    for (var i = 0; i < length; i++) {
      _syncField(i);
    }
    notifyListeners();
  }

  void _syncField(int index, {bool force = false}) {
    final wanted = _fieldValue(_digits[index]);
    final field = fieldControllers[index];
    if (force || field.text != wanted.text) field.value = wanted;
  }

  static TextEditingValue _fieldValue(String digit) {
    final text = '$fieldSentinel$digit';
    return TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: text.length),
    );
  }

  @override
  void dispose() {
    _resendTimer?.cancel();
    for (final node in focusNodes) {
      node.dispose();
    }
    for (final field in fieldControllers) {
      field.dispose();
    }
    super.dispose();
  }
}
