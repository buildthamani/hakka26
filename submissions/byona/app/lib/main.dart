import 'package:flutter/material.dart';

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
      theme: ThemeData(brightness: Brightness.light),
      darkTheme: ThemeData(brightness: Brightness.dark),
      home: const OtpScreen(),
    );
  }
}
