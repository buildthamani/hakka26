import 'dart:convert';
import 'dart:io';
import 'dart:ui';

import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/otp/otp_colors.dart';

Map<String, Color> byJsonKey(OtpColors colors) => {
  'sheet': colors.sheet,
  'scrim': colors.scrim,
  'text_primary': colors.textPrimary,
  'text_secondary': colors.textSecondary,
  'digit_filled_bg': colors.digitFilledBg,
  'digit_filled_text': colors.digitFilledText,
  'digit_focused_bg': colors.digitFocusedBg,
  'digit_focused_outline': colors.digitFocusedOutline,
  'digit_empty_bg': colors.digitEmptyBg,
  'digit_empty_outline': colors.digitEmptyOutline,
  'digit_placeholder': colors.digitPlaceholder,
  'button_bg': colors.buttonBg,
  'button_text': colors.buttonText,
  'button_disabled_bg': colors.buttonDisabledBg,
  'link': colors.link,
  'success': colors.success,
  'error': colors.error,
};

int argbFromHex(String hex) {
  final digits = hex.substring(1);
  return int.parse(digits.length == 6 ? 'FF$digits' : digits, radix: 16);
}

void main() {
  final palette =
      jsonDecode(File('assets/colours.json').readAsStringSync())['otp']
          as Map<String, dynamic>;

  for (final (name, colors) in [
    ('light', OtpColors.light),
    ('dark', OtpColors.dark),
  ]) {
    test('the $name constants match colours.json', () {
      final expected = (palette[name] as Map<String, dynamic>).map(
        (key, hex) => MapEntry(key, argbFromHex(hex as String)),
      );
      final actual = byJsonKey(
        colors,
      ).map((key, color) => MapEntry(key, color.toARGB32()));

      expect(actual, expected);
    });
  }
}
