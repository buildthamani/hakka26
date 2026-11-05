import 'package:flutter/material.dart';

import 'otp/otp_colors.dart';
import 'otp/otp_sheet.dart';

const appName = 'hakkahakka26';

void main() {
  runApp(const HakkaApp());
}

class HakkaApp extends StatelessWidget {
  const HakkaApp({super.key, this.home});

  /// Overridable for tests.
  final Widget? home;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: appName,
      debugShowCheckedModeBanner: false,
      themeMode: ThemeMode.system,
      theme: ThemeData(
        brightness: Brightness.light,
        colorSchemeSeed: OtpColors.light.buttonBg,
      ),
      darkTheme: ThemeData(
        brightness: Brightness.dark,
        colorSchemeSeed: OtpColors.dark.buttonBg,
      ),
      home: home ?? const OtpScreen(),
    );
  }
}
