import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/main.dart';
import 'package:hakka/otp/otp_input.dart';
import 'package:hakka/otp/otp_screen.dart';
import 'package:hakka/theme/otp_colors.dart';

final palette = OtpPalette.fromJsonString(
  File('assets/colours.json').readAsStringSync(),
);

Finder digitBox(int n) => find.byKey(ValueKey('otp_digit_$n'));
Finder get submit => find.byKey(const ValueKey('otp_submit'));
Finder get resend => find.byKey(const ValueKey('otp_resend'));

String digitText(WidgetTester tester, int n) =>
    tester.widget<TextField>(digitBox(n)).controller!.text;

bool hasFocus(WidgetTester tester, int n) =>
    tester.widget<TextField>(digitBox(n)).focusNode!.hasFocus;

bool submitEnabled(WidgetTester tester) =>
    tester.widget<FilledButton>(submit).onPressed != null;

String statusText(WidgetTester tester) =>
    tester.widget<Text>(find.byKey(const ValueKey('otp_status'))).data!;

Future<void> pumpApp(
  WidgetTester tester, {
  ThemeMode themeMode = ThemeMode.light,
}) async {
  await tester.pumpWidget(HakkaApp(palette: palette, themeMode: themeMode));
  await tester.pump();
}

Future<void> typeCode(WidgetTester tester, String code) async {
  for (final char in code.split('')) {
    final focused = List.generate(
      6,
      (i) => i + 1,
    ).firstWhere((n) => hasFocus(tester, n));
    await tester.enterText(digitBox(focused), char);
    await tester.pump();
  }
}

void main() {
  testWidgets('shows the copy and six empty boxes with the first focused', (
    tester,
  ) async {
    await pumpApp(tester);

    expect(find.text(OtpCopy.title), findsOneWidget);
    expect(find.text(OtpCopy.subtitle), findsOneWidget);
    expect(find.text(OtpCopy.submit), findsOneWidget);
    expect(find.byType(OtpDigitBox), findsNWidgets(6));
    for (var n = 1; n <= 6; n++) {
      expect(digitText(tester, n), isEmpty);
    }
    expect(hasFocus(tester, 1), isTrue);
    expect(find.text('0'), findsNothing);
  });

  testWidgets('each box is labelled for screen readers', (tester) async {
    final semantics = tester.ensureSemantics();
    await pumpApp(tester);
    for (var n = 1; n <= 6; n++) {
      expect(find.bySemanticsLabel(RegExp('Digit $n of 6')), findsOneWidget);
    }
    semantics.dispose();
  });

  testWidgets('typing advances focus and rejects non-digits', (tester) async {
    await pumpApp(tester);

    await tester.enterText(digitBox(1), 'a');
    await tester.pump();
    expect(digitText(tester, 1), isEmpty);
    expect(hasFocus(tester, 1), isTrue);

    await tester.enterText(digitBox(1), '7');
    await tester.pump();
    expect(digitText(tester, 1), '7');
    expect(hasFocus(tester, 2), isTrue);
  });

  testWidgets('backspace on an empty box clears the previous digit', (
    tester,
  ) async {
    await pumpApp(tester);
    await typeCode(tester, '21');
    expect(hasFocus(tester, 3), isTrue);

    await tester.sendKeyEvent(LogicalKeyboardKey.backspace);
    await tester.pump();

    expect(hasFocus(tester, 2), isTrue);
    expect(digitText(tester, 2), isEmpty);
    expect(digitText(tester, 1), '2');
  });

  testWidgets('pasting text with a code fills all six boxes', (tester) async {
    await pumpApp(tester);
    await tester.enterText(digitBox(1), 'Your code is 482913');
    await tester.pump();

    expect(
      [for (var n = 1; n <= 6; n++) digitText(tester, n)].join(),
      '482913',
    );
    expect(submitEnabled(tester), isTrue);
  });

  testWidgets('submit is disabled until all six digits are filled', (
    tester,
  ) async {
    await pumpApp(tester);
    expect(submitEnabled(tester), isFalse);
    await typeCode(tester, '12345');
    expect(submitEnabled(tester), isFalse);
    await typeCode(tester, '6');
    expect(submitEnabled(tester), isTrue);
  });

  testWidgets('the correct code shows the success message', (tester) async {
    await pumpApp(tester);
    await typeCode(tester, '123456');
    await tester.tap(submit);
    await tester.pump();

    expect(statusText(tester), "You're in.");
  });

  testWidgets('a wrong code shows the error, clears and refocuses', (
    tester,
  ) async {
    await pumpApp(tester);
    await typeCode(tester, '654321');
    await tester.tap(submit);
    await tester.pump();

    expect(statusText(tester), "That code didn't work. Try again.");
    for (var n = 1; n <= 6; n++) {
      expect(digitText(tester, n), isEmpty);
    }
    expect(hasFocus(tester, 1), isTrue);
    expect(submitEnabled(tester), isFalse);
  });

  testWidgets('resend counts down 30 s, then restarts when tapped', (
    tester,
  ) async {
    await pumpApp(tester);
    expect(find.text('Resend in 0:30'), findsOneWidget);

    await tester.tap(resend);
    await tester.pump(const Duration(seconds: 1));
    expect(find.text('Resend in 0:29'), findsOneWidget);

    await tester.pump(const Duration(seconds: 29));
    expect(find.text('Resend'), findsOneWidget);

    await tester.tap(resend);
    await tester.pump();
    expect(find.text('Resend in 0:30'), findsOneWidget);
  });

  testWidgets('uses the dark palette in dark mode', (tester) async {
    await pumpApp(tester, themeMode: ThemeMode.dark);
    final context = tester.element(find.byType(OtpInput));
    expect(OtpColors.of(context).sheet, palette.dark.sheet);
  });
}
