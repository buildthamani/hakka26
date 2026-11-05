import 'package:flutter/material.dart';

/// OTP palette, mirrored from assets/colours.json (`otp.light` / `otp.dark`).
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

  static const light = OtpColors(
    sheet: Color(0xFFFFFFFF),
    scrim: Color(0x99000000),
    textPrimary: Color(0xFF111111),
    textSecondary: Color(0xFF5F6368),
    digitFilledBg: Color(0xFF12361B),
    digitFilledText: Color(0xFFFFFFFF),
    digitFocusedBg: Color(0xFFFFFFFF),
    digitFocusedOutline: Color(0xFF111111),
    digitEmptyBg: Color(0xFFF1F1F1),
    digitEmptyOutline: Color(0xFFE0E0E0),
    digitPlaceholder: Color(0xFF9E9E9E),
    buttonBg: Color(0xFF12361B),
    buttonText: Color(0xFFFFFFFF),
    buttonDisabledBg: Color(0xFFC9D1CB),
    link: Color(0xFF12361B),
    success: Color(0xFF1B7F3A),
    error: Color(0xFFC62828),
  );

  static const dark = OtpColors(
    sheet: Color(0xFF161A17),
    scrim: Color(0xCC000000),
    textPrimary: Color(0xFFF5F5F5),
    textSecondary: Color(0xFFB0B5B1),
    digitFilledBg: Color(0xFF2E7D45),
    digitFilledText: Color(0xFFFFFFFF),
    digitFocusedBg: Color(0xFF161A17),
    digitFocusedOutline: Color(0xFFF5F5F5),
    digitEmptyBg: Color(0xFF262B27),
    digitEmptyOutline: Color(0xFF353B36),
    digitPlaceholder: Color(0xFF6F7570),
    buttonBg: Color(0xFF2E7D45),
    buttonText: Color(0xFFFFFFFF),
    buttonDisabledBg: Color(0xFF2A302B),
    link: Color(0xFF7BC795),
    success: Color(0xFF7BC795),
    error: Color(0xFFEF9A9A),
  );

  static OtpColors of(BuildContext context) =>
      Theme.of(context).brightness == Brightness.dark ? dark : light;
}
