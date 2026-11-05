import 'package:flutter/material.dart';

import 'otp/otp_screen.dart';
import 'otp/palette.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final palettes = await OtpPalettes.load();
  runApp(HakkaApp(palettes: palettes));
}

class HakkaApp extends StatelessWidget {
  const HakkaApp({
    super.key,
    required this.palettes,
    this.themeMode = ThemeMode.system,
  });

  final OtpPalettes palettes;
  final ThemeMode themeMode;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'hakkahakka26',
      debugShowCheckedModeBanner: false,
      themeMode: themeMode,
      theme: ThemeData(
        brightness: Brightness.light,
        useMaterial3: true,
        colorSchemeSeed: const Color(0xFF12361B),
      ),
      darkTheme: ThemeData(
        brightness: Brightness.dark,
        useMaterial3: true,
        colorSchemeSeed: const Color(0xFF2E7D45),
      ),
      home: OtpScreen(palettes: palettes),
    );
  }
}
