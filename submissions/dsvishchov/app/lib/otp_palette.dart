import 'dart:convert';

import 'package:flutter/services.dart';

class OtpColors {
  OtpColors(this.light, this.dark);
  final OtpPalette light;
  final OtpPalette dark;

  static Future<OtpColors> load() async {
    final json =
        jsonDecode(await rootBundle.loadString('assets/colours.json'))
            as Map<String, dynamic>;
    final otp = json['otp'] as Map<String, dynamic>;
    return OtpColors(
      OtpPalette.fromJson(otp['light']),
      OtpPalette.fromJson(otp['dark']),
    );
  }

  OtpPalette forBrightness(Brightness brightness) =>
      brightness == Brightness.dark ? dark : light;
}

class OtpPalette {
  OtpPalette(this._colors);
  factory OtpPalette.fromJson(Map<String, dynamic> json) => OtpPalette(
    json.map((key, value) {
      final hex = (value as String).substring(1);
      return MapEntry(
        key,
        Color(int.parse(hex.length == 6 ? 'FF$hex' : hex, radix: 16)),
      );
    }),
  );
  final Map<String, Color> _colors;
  Color operator [](String name) => _colors[name]!;
}
