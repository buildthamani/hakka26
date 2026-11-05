import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'otp_verifier.dart';

enum OtpStatus { idle, verifying, success, error }

/// Holds all OTP screen state: the digits, which box has focus, the
/// verification result and the resend countdown.
class OtpController extends ChangeNotifier {
  OtpController({
    required OtpVerifier verifier,
    this.length = 6,
    this.resendCooldown = const Duration(seconds: 30),
  }) : _verifier = verifier,
       _digits = List.filled(length, ''),
       textControllers = List.generate(length, (_) => TextEditingController()) {
    focusNodes = List.generate(
      length,
      (index) => FocusNode(
        debugLabel: 'otp_digit_${index + 1}',
        onKeyEvent: (_, event) => _handleKey(index, event),
      ),
    );
    _startCountdown();
  }

  final int length;
  final Duration resendCooldown;
  final OtpVerifier _verifier;
  final List<String> _digits;
  final List<TextEditingController> textControllers;
  late final List<FocusNode> focusNodes;

  OtpStatus _status = OtpStatus.idle;
  int _resendSecondsLeft = 0;
  Timer? _timer;

  List<String> get digits => List.unmodifiable(_digits);
  String get code => _digits.join();
  bool get isComplete => _digits.every((d) => d.isNotEmpty);
  OtpStatus get status => _status;
  int get resendSecondsLeft => _resendSecondsLeft;
  bool get canResend => _resendSecondsLeft == 0;
  bool get canSubmit => isComplete && _status != OtpStatus.verifying;

  /// Called whenever the text in box [index] changes (typing or pasting).
  void onBoxChanged(int index, String rawValue) {
    var entered = rawValue.replaceAll(RegExp(r'\D'), '');
    final previous = _digits[index];

    // Typing into an already-filled box: keep only the newly typed digit.
    if (previous.isNotEmpty && entered.length == 2) {
      entered = entered.startsWith(previous)
          ? entered.substring(1)
          : entered.substring(0, 1);
    }

    if (entered.isEmpty) {
      _setDigit(index, '');
    } else if (entered.length >= length) {
      // A full code was pasted: fill every box from the start.
      _fillFrom(0, entered.substring(0, length));
    } else {
      _fillFrom(index, entered);
    }
    _sync();
  }

  /// Clears every box and moves focus back to the first one.
  void clear() {
    _digits.fillRange(0, length, '');
    _sync();
    focusBox(0);
  }

  void focusBox(int index) => focusNodes[index].requestFocus();

  Future<void> submit() async {
    if (!canSubmit) return;
    _status = OtpStatus.verifying;
    notifyListeners();

    final ok = await _verifier.verify(code);
    _status = ok ? OtpStatus.success : OtpStatus.error;
    if (ok) {
      focusNodes.forEach((node) => node.unfocus());
      notifyListeners();
    } else {
      clear();
    }
  }

  void resend() {
    if (!canResend) return;
    _status = OtpStatus.idle;
    _startCountdown();
  }

  void _fillFrom(int start, String digits) {
    var index = start;
    for (final digit in digits.split('')) {
      if (index >= length) break;
      _digits[index++] = digit;
    }
    focusBox(index.clamp(0, length - 1));
  }

  void _setDigit(int index, String digit) => _digits[index] = digit;

  KeyEventResult _handleKey(int index, KeyEvent event) {
    final isBackspace = event.logicalKey == LogicalKeyboardKey.backspace;
    final isPress = event is KeyDownEvent || event is KeyRepeatEvent;
    if (!isBackspace || !isPress) return KeyEventResult.ignored;

    // Backspace on an empty box: step back and clear the previous digit.
    if (_digits[index].isEmpty && index > 0) {
      _setDigit(index - 1, '');
      _sync();
      focusBox(index - 1);
      return KeyEventResult.handled;
    }
    return KeyEventResult.ignored;
  }

  /// Pushes the digit model into the text fields and notifies listeners.
  void _sync() {
    for (var i = 0; i < length; i++) {
      final text = _digits[i];
      if (textControllers[i].text != text) {
        textControllers[i].value = TextEditingValue(
          text: text,
          selection: TextSelection.collapsed(offset: text.length),
        );
      }
    }
    notifyListeners();
  }

  void _startCountdown() {
    _timer?.cancel();
    _resendSecondsLeft = resendCooldown.inSeconds;
    notifyListeners();
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      _resendSecondsLeft--;
      if (_resendSecondsLeft <= 0) {
        _resendSecondsLeft = 0;
        timer.cancel();
      }
      notifyListeners();
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    for (final c in textControllers) {
      c.dispose();
    }
    for (final n in focusNodes) {
      n.dispose();
    }
    super.dispose();
  }
}
