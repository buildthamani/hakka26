import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart' show rootBundle;

/// OTP screen colours, read from `assets/colours.json` and exposed through the
/// theme so light and dark mode each get their own palette.
@immutable
class OtpColors extends ThemeExtension<OtpColors> {
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
    Color c(String key) => parseHexColor(json[key] as String);
    return OtpColors(
      sheet: c('sheet'),
      scrim: c('scrim'),
      textPrimary: c('text_primary'),
      textSecondary: c('text_secondary'),
      digitFilledBg: c('digit_filled_bg'),
      digitFilledText: c('digit_filled_text'),
      digitFocusedBg: c('digit_focused_bg'),
      digitFocusedOutline: c('digit_focused_outline'),
      digitEmptyBg: c('digit_empty_bg'),
      digitEmptyOutline: c('digit_empty_outline'),
      digitPlaceholder: c('digit_placeholder'),
      buttonBg: c('button_bg'),
      buttonText: c('button_text'),
      buttonDisabledBg: c('button_disabled_bg'),
      link: c('link'),
      success: c('success'),
      error: c('error'),
    );
  }

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

  static OtpColors of(BuildContext context) =>
      Theme.of(context).extension<OtpColors>()!;

  @override
  OtpColors copyWith() => this;

  @override
  OtpColors lerp(OtpColors? other, double t) {
    if (other == null) return this;
    Color l(Color a, Color b) => Color.lerp(a, b, t)!;
    return OtpColors(
      sheet: l(sheet, other.sheet),
      scrim: l(scrim, other.scrim),
      textPrimary: l(textPrimary, other.textPrimary),
      textSecondary: l(textSecondary, other.textSecondary),
      digitFilledBg: l(digitFilledBg, other.digitFilledBg),
      digitFilledText: l(digitFilledText, other.digitFilledText),
      digitFocusedBg: l(digitFocusedBg, other.digitFocusedBg),
      digitFocusedOutline: l(digitFocusedOutline, other.digitFocusedOutline),
      digitEmptyBg: l(digitEmptyBg, other.digitEmptyBg),
      digitEmptyOutline: l(digitEmptyOutline, other.digitEmptyOutline),
      digitPlaceholder: l(digitPlaceholder, other.digitPlaceholder),
      buttonBg: l(buttonBg, other.buttonBg),
      buttonText: l(buttonText, other.buttonText),
      buttonDisabledBg: l(buttonDisabledBg, other.buttonDisabledBg),
      link: l(link, other.link),
      success: l(success, other.success),
      error: l(error, other.error),
    );
  }
}

/// Light and dark OTP palettes.
class OtpPalette {
  const OtpPalette({required this.light, required this.dark});

  /// Parses the contents of `colours.json`.
  factory OtpPalette.fromJsonString(String source) {
    final otp =
        (jsonDecode(source) as Map<String, dynamic>)['otp']
            as Map<String, dynamic>;
    return OtpPalette(
      light: OtpColors.fromJson(otp['light'] as Map<String, dynamic>),
      dark: OtpColors.fromJson(otp['dark'] as Map<String, dynamic>),
    );
  }

  static const assetPath = 'assets/colours.json';

  static Future<OtpPalette> load() async =>
      OtpPalette.fromJsonString(await rootBundle.loadString(assetPath));

  final OtpColors light;
  final OtpColors dark;
}

/// Parses `#RRGGBB` or `#AARRGGBB`.
Color parseHexColor(String hex) {
  var value = hex.replaceFirst('#', '');
  if (value.length == 6) value = 'FF$value';
  return Color(int.parse(value, radix: 16));
}
