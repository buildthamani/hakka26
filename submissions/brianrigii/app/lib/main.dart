import 'package:flutter/material.dart';

import 'otp/otp_colours.dart';
import 'otp/otp_screen.dart';

const appName = 'hakkahakka26';

void main() {
  runApp(const HakkaApp());
}

class HakkaApp extends StatelessWidget {
  const HakkaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: appName,
      debugShowCheckedModeBanner: false,
      themeMode: ThemeMode.system,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: OtpColours.light.buttonBg,
          brightness: Brightness.light,
        ),
      ),
      darkTheme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: OtpColours.dark.buttonBg,
          brightness: Brightness.dark,
        ),
      ),
      home: const OtpScreen(),
    );
  }
}
