import 'package:flutter/material.dart';

import 'otp_palette.dart';
import 'otp_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(HakkaApp(colors: await OtpColors.load()));
}

class HakkaApp extends StatelessWidget {
  const HakkaApp({
    super.key,
    required this.colors,
    this.themeMode = ThemeMode.system,
    this.now,
  });

  final OtpColors colors;
  final ThemeMode themeMode;
  final DateTime Function()? now;

  @override
  Widget build(BuildContext context) {
    ThemeData theme(Brightness brightness) => ThemeData(
      brightness: brightness,
      fontFamily: 'Roboto',
      colorScheme: ColorScheme.fromSeed(
        seedColor: colors.forBrightness(brightness)['button_bg'],
        brightness: brightness,
      ),
      useMaterial3: true,
    );
    return MaterialApp(
      title: 'Thamani verification',
      debugShowCheckedModeBanner: false,
      theme: theme(Brightness.light),
      darkTheme: theme(Brightness.dark),
      themeMode: themeMode,
      home: Builder(
        builder: (context) => OtpScreen(
          palette: colors.forBrightness(Theme.of(context).brightness),
          now: now,
        ),
      ),
    );
  }
}
