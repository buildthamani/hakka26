import 'package:flutter/material.dart';

import 'otp/otp_screen.dart';
import 'otp/otp_verifier.dart';
import 'theme/otp_colors.dart';

const appName = 'hakkahakka26';

/// The code the demo verifier accepts.
const demoOtpCode = '123456';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final palette = await OtpPalette.load();
  runApp(HakkaApp(palette: palette));
}

class HakkaApp extends StatelessWidget {
  const HakkaApp({
    super.key,
    required this.palette,
    this.verifier = const FixedCodeOtpVerifier(demoOtpCode),
    this.themeMode = ThemeMode.system,
  });

  final OtpPalette palette;
  final OtpVerifier verifier;
  final ThemeMode themeMode;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: appName,
      debugShowCheckedModeBanner: false,
      themeMode: themeMode,
      theme: _theme(Brightness.light, palette.light),
      darkTheme: _theme(Brightness.dark, palette.dark),
      home: OtpScreen(verifier: verifier),
    );
  }

  ThemeData _theme(Brightness brightness, OtpColors colors) => ThemeData(
    brightness: brightness,
    colorScheme: ColorScheme.fromSeed(
      seedColor: colors.buttonBg,
      brightness: brightness,
    ),
    extensions: [colors],
  );
}
