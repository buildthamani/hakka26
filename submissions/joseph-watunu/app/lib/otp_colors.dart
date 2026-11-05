import 'dart:convert';

import 'package:flutter/services.dart';

/// Both themes read directly from the supplied design tokens.
class OtpColors {
  OtpColors._(this.light, this.dark);

  final OtpPalette light;
  final OtpPalette dark;

  static Future<OtpColors> load() async {
    final json =
        jsonDecode(await rootBundle.loadString('assets/colours.json'))
            as Map<String, dynamic>;
    final otp = json['otp'] as Map<String, dynamic>;
    return OtpColors._(
      OtpPalette(otp['light'] as Map<String, dynamic>),
      OtpPalette(otp['dark'] as Map<String, dynamic>),
    );
  }

  OtpPalette of(Brightness brightness) =>
      brightness == Brightness.dark ? dark : light;
}

class OtpPalette {
  OtpPalette(Map<String, dynamic> tokens)
    : _colors = tokens.map((name, value) {
        final hex = (value as String).replaceFirst('#', '');
        return MapEntry(
          name,
          Color(int.parse(hex.length == 6 ? 'FF$hex' : hex, radix: 16)),
        );
      });

  final Map<String, Color> _colors;

  Color operator [](String name) => _colors[name]!;
}
