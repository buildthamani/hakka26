import 'dart:ui' show ImageFilter;

import 'package:flutter/material.dart';

import 'otp_screen.dart';
import 'otp_theme.dart';

const appName = 'hakkahakka26';

// The reference dims the blurred page only lightly; the scrim token at full opacity hides it.
const _scrimStrength = 0.18;

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final palette = await OtpPalette.load();
  runApp(HakkaApp(palette: palette));
}

class HakkaApp extends StatelessWidget {
  final OtpPalette palette;

  const HakkaApp({super.key, required this.palette});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: appName,
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.light,
        colorSchemeSeed: palette.light.buttonBg,
      ),
      darkTheme: ThemeData(
        brightness: Brightness.dark,
        colorSchemeSeed: palette.dark.buttonBg,
      ),
      home: HomeScreen(palette: palette),
    );
  }
}

class HomeScreen extends StatelessWidget {
  final OtpPalette palette;

  const HomeScreen({super.key, required this.palette});

  @override
  Widget build(BuildContext context) {
    final colors = palette.forBrightness(Theme.of(context).brightness);
    final keyboardOpen = MediaQuery.viewInsetsOf(context).bottom > 0;

    return Scaffold(
      backgroundColor: colors.sheet,
      body: Stack(
        fit: StackFit.expand,
        children: [
          ImageFiltered(
            imageFilter: ImageFilter.blur(sigmaX: 6, sigmaY: 6),
            child: _WelcomeBackPage(colors: colors),
          ),
          ColoredBox(
            color: colors.scrim.withValues(
              alpha: colors.scrim.a * _scrimStrength,
            ),
          ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
              child: Align(
                alignment: Alignment.bottomCenter,
                child: FractionallySizedBox(
                  widthFactor: 1,
                  heightFactor: keyboardOpen ? 1 : 0.85,
                  child: OtpScreen(colors: colors),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _WelcomeBackPage extends StatelessWidget {
  final OtpColors colors;

  const _WelcomeBackPage({required this.colors});

  @override
  Widget build(BuildContext context) {
    Widget block(double height) => Container(
      height: height,
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: colors.digitEmptyBg,
        borderRadius: BorderRadius.circular(14),
      ),
    );

    return ExcludeSemantics(
      child: ColoredBox(
        color: colors.sheet,
        child: SafeArea(
          child: ListView(
            physics: const NeverScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(24, 72, 24, 24),
            children: [
              Text(
                'Welcome back',
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: colors.textPrimary,
                  fontSize: 24,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 140),
              block(52),
              block(52),
              const SizedBox(height: 60),
              block(52),
            ],
          ),
        ),
      ),
    );
  }
}
