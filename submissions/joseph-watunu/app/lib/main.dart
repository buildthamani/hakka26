import 'package:flutter/material.dart';

import 'otp_colors.dart';
import 'otp_screen.dart';

const appName = 'hakkahakka26';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(HakkaApp(colors: await OtpColors.load()));
}

class HakkaApp extends StatelessWidget {
  const HakkaApp({
    super.key,
    required this.colors,
    this.themeMode = ThemeMode.system,
  });

  final OtpColors colors;
  final ThemeMode themeMode;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: appName,
      debugShowCheckedModeBanner: false,
      themeMode: themeMode,
      theme: ThemeData(brightness: Brightness.light),
      darkTheme: ThemeData(brightness: Brightness.dark),
      home: OtpScreen(colors: colors),
    );
  }
}
