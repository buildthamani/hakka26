import 'dart:convert';
import 'dart:io';
import 'dart:ui';

import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/otp/otp_colors.dart';

Color _parse(String hex) {
  final value = hex.replaceFirst('#', '');
  return Color(int.parse(value.length == 6 ? 'FF$value' : value, radix: 16));
}

void main() {
  final json = jsonDecode(File('assets/colours.json').readAsStringSync()) as Map<String, dynamic>;
  final otp = json['otp'] as Map<String, dynamic>;

  for (final (mode, palette) in [('light', OtpColors.light), ('dark', OtpColors.dark)]) {
    test('$mode palette matches assets/colours.json', () {
      final expected = (otp[mode] as Map<String, dynamic>).map((key, value) => MapEntry(key, _parse(value as String)));
      expect(palette.toJsonKeys(), expected);
    });
  }
}
