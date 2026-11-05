import 'dart:convert';

import 'package:flutter/services.dart';

class OtpColors {
  final Color sheet;
  final Color scrim;
  final Color textPrimary;
  final Color textSecondary;
  final Color digitFilledBg;
  final Color digitFilledText;
  final Color digitFocusedBg;
  final Color digitFocusedOutline;
  final Color digitEmptyBg;
  final Color digitEmptyOutline;
  final Color digitPlaceholder;
  final Color buttonBg;
  final Color buttonText;
  final Color buttonDisabledBg;
  final Color link;
  final Color success;
  final Color error;

  const OtpColors({
    required this.sheet,
    required this.scrim,
    required this.textPrimary,
    required this.textSecondary,
    required this.digitFilledBg,
    required this.digitFilledText,
    required this.digitFocusedBg,
    required this.digitFocusedOutline,
    required this.digitEmptyBg,
    required this.digitEmptyOutline,
    required this.digitPlaceholder,
    required this.buttonBg,
    required this.buttonText,
    required this.buttonDisabledBg,
    required this.link,
    required this.success,
    required this.error,
  });

  factory OtpColors.fromJson(Map<String, dynamic> json) {
    Color hex(String key) {
      final value = (json[key] as String).replaceFirst('#', '');
      final argb = value.length == 6 ? 'FF$value' : value;
      return Color(int.parse(argb, radix: 16));
    }

    return OtpColors(
      sheet: hex('sheet'),
      scrim: hex('scrim'),
      textPrimary: hex('text_primary'),
      textSecondary: hex('text_secondary'),
      digitFilledBg: hex('digit_filled_bg'),
      digitFilledText: hex('digit_filled_text'),
      digitFocusedBg: hex('digit_focused_bg'),
      digitFocusedOutline: hex('digit_focused_outline'),
      digitEmptyBg: hex('digit_empty_bg'),
      digitEmptyOutline: hex('digit_empty_outline'),
      digitPlaceholder: hex('digit_placeholder'),
      buttonBg: hex('button_bg'),
      buttonText: hex('button_text'),
      buttonDisabledBg: hex('button_disabled_bg'),
      link: hex('link'),
      success: hex('success'),
      error: hex('error'),
    );
  }
}

class OtpPalette {
  final OtpColors light;
  final OtpColors dark;

  const OtpPalette({required this.light, required this.dark});

  static Future<OtpPalette> load() async {
    final raw = await rootBundle.loadString('assets/colours.json');
    final data = jsonDecode(raw) as Map<String, dynamic>;
    final otp = data['otp'] as Map<String, dynamic>;
    return OtpPalette(
      light: OtpColors.fromJson(otp['light'] as Map<String, dynamic>),
      dark: OtpColors.fromJson(otp['dark'] as Map<String, dynamic>),
    );
  }

  OtpColors forBrightness(Brightness brightness) =>
      brightness == Brightness.dark ? dark : light;
}
