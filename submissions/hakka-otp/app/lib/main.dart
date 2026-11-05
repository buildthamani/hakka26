import 'package:flutter/material.dart';

import 'otp/otp_colors.dart';
import 'otp/otp_screen.dart';

void main() {
  runApp(const HakkaApp());
}

class HakkaApp extends StatelessWidget {
  const HakkaApp({super.key, this.themeMode = ThemeMode.system});

  final ThemeMode themeMode;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'hakkahakka26',
      debugShowCheckedModeBanner: false,
      themeMode: themeMode,
      theme: _theme(Brightness.light, OtpColors.light),
      darkTheme: _theme(Brightness.dark, OtpColors.dark),
      home: const OtpScreen(),
    );
  }

  static ThemeData _theme(Brightness brightness, OtpColors colors) => ThemeData(
        brightness: brightness,
        colorScheme: ColorScheme.fromSeed(seedColor: colors.buttonBg, brightness: brightness),
        extensions: [colors],
      );
}
