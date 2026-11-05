import 'dart:math' as math;
import 'package:flutter/widgets.dart';

/// The native editing buffer is also the OTP state; there is no mirrored list.
class OtpController extends TextEditingController {
  static const length = 6;
  OtpController()
    : super.fromValue(
        const TextEditingValue(selection: TextSelection.collapsed(offset: 0)),
      );

  String get code => text;
  bool get isComplete => text.length == length;
  int get activeIndex => math.min(math.max(0, selection.start), length - 1);
  String digitAt(int index) => index < text.length ? text[index] : '';

  void select(int index) {
    final start = index.clamp(0, math.min(text.length, length - 1)).toInt();
    selection = TextSelection(
      baseOffset: start,
      extentOffset: math.min(start + 1, text.length),
    );
  }

  void enter(int index, String raw) {
    select(index);
    final start = selection.start;
    value = formatEdit(
      value,
      TextEditingValue(
        text: text.replaceRange(start, selection.end, raw),
        selection: TextSelection.collapsed(offset: start + raw.length),
      ),
    );
  }

  /// Remove the selected digit, or the preceding digit at the end of the code.
  void backspace(int index) {
    select(index);
    final start = selection.isCollapsed
        ? math.max(0, selection.start - 1)
        : selection.start;
    value = TextEditingValue(
      text: text.replaceRange(start, selection.end, ''),
      selection: TextSelection.collapsed(offset: start),
    );
  }

  static String _digits(String text) => text.replaceAll(RegExp(r'[^0-9]'), '');

  static TextEditingValue formatEdit(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final start = oldValue.selection.start.clamp(0, oldValue.text.length);
    final end = oldValue.selection.end.clamp(start, oldValue.text.length);
    final prefix = oldValue.text.substring(0, start);
    final suffix = oldValue.text.substring(end);
    final isReplacement =
        newValue.text.startsWith(prefix) &&
        newValue.text.endsWith(suffix) &&
        newValue.text.length >= prefix.length + suffix.length;
    final inserted = isReplacement
        ? newValue.text.substring(
            prefix.length,
            newValue.text.length - suffix.length,
          )
        : newValue.text;
    final insertedDigits = _digits(inserted);
    if (inserted.isNotEmpty && insertedDigits.isEmpty) return oldValue;
    // A complete pasted/autofilled code replaces the entire OTP at any position.
    if (insertedDigits.length >= length) {
      return TextEditingValue(
        text: insertedDigits.substring(0, length),
        selection: const TextSelection(
          baseOffset: length - 1,
          extentOffset: length,
        ),
      );
    }
    final digits = _digits(newValue.text);
    final clean = digits.substring(0, math.min(length, digits.length));
    final rawCaret = newValue.selection.start.clamp(0, newValue.text.length);
    final caret = math.min(
      clean.length,
      _digits(newValue.text.substring(0, rawCaret)).length,
    );
    return TextEditingValue(
      text: clean,
      selection: TextSelection(
        baseOffset: caret,
        extentOffset: insertedDigits.isNotEmpty
            ? math.min(caret + 1, clean.length)
            : caret,
      ),
    );
  }
}
