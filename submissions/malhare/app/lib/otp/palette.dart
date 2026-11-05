import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Colours for the OTP sheet, parsed from `assets/colours.json`.
class OtpColors {
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
    return OtpColors(
      sheet: _color(json['sheet']),
      scrim: _color(json['scrim']),
      textPrimary: _color(json['text_primary']),
      textSecondary: _color(json['text_secondary']),
      digitFilledBg: _color(json['digit_filled_bg']),
      digitFilledText: _color(json['digit_filled_text']),
      digitFocusedBg: _color(json['digit_focused_bg']),
      digitFocusedOutline: _color(json['digit_focused_outline']),
      digitEmptyBg: _color(json['digit_empty_bg']),
      digitEmptyOutline: _color(json['digit_empty_outline']),
      digitPlaceholder: _color(json['digit_placeholder']),
      buttonBg: _color(json['button_bg']),
      buttonText: _color(json['button_text']),
      buttonDisabledBg: _color(json['button_disabled_bg']),
      link: _color(json['link']),
      success: _color(json['success']),
      error: _color(json['error']),
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
}

/// Thamani brand colours, used for the backdrop behind the sheet.
class BrandColors {
  const BrandColors({
    required this.fogInk,
    required this.clearWhite,
    required this.thamaniBlue,
    required this.signalBlue,
    required this.clearCyan,
    required this.acidLemon,
  });

  factory BrandColors.fromJson(Map<String, dynamic> json) {
    return BrandColors(
      fogInk: _color(json['fog_ink']),
      clearWhite: _color(json['clear_white']),
      thamaniBlue: _color(json['thamani_blue']),
      signalBlue: _color(json['signal_blue']),
      clearCyan: _color(json['clear_cyan']),
      acidLemon: _color(json['acid_lemon']),
    );
  }

  final Color fogInk;
  final Color clearWhite;
  final Color thamaniBlue;
  final Color signalBlue;
  final Color clearCyan;
  final Color acidLemon;
}

class OtpPalettes {
  const OtpPalettes({
    required this.light,
    required this.dark,
    required this.brand,
  });

  /// Reads the palette from the asset bundle. `assets/colours.json` is declared
  /// in `pubspec.yaml`, so a missing asset is a build-time error.
  static Future<OtpPalettes> load({AssetBundle? bundle}) async {
    final raw = await (bundle ?? rootBundle).loadString('assets/colours.json');
    final json = jsonDecode(raw) as Map<String, dynamic>;
    final otp = json['otp'] as Map<String, dynamic>;
    return OtpPalettes(
      light: OtpColors.fromJson(otp['light'] as Map<String, dynamic>),
      dark: OtpColors.fromJson(otp['dark'] as Map<String, dynamic>),
      brand: BrandColors.fromJson(json['thamani'] as Map<String, dynamic>),
    );
  }

  final OtpColors light;
  final OtpColors dark;
  final BrandColors brand;
}

Color _color(Object? value) {
  final hex = (value as String).replaceFirst('#', '');
  final argb = hex.length == 6 ? 'FF$hex' : hex;
  return Color(int.parse(argb, radix: 16));
}
