import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/main.dart';

Finder digit(int n) => find.byKey(Key('otp_digit_$n'));

String digitText(WidgetTester tester, int n) => tester.widget<TextField>(digit(n)).controller!.text;

bool hasFocus(WidgetTester tester, int n) => tester.widget<TextField>(digit(n)).focusNode!.hasFocus;

bool submitEnabled(WidgetTester tester) =>
    tester.widget<FilledButton>(find.byKey(const Key('otp_submit'))).onPressed != null;

Future<void> pumpApp(WidgetTester tester) async {
  await tester.pumpWidget(const HakkaApp());
  await tester.pump();
}

void main() {
  testWidgets('shows the exact copy and six empty boxes', (tester) async {
    await pumpApp(tester);

    expect(find.text('Enter your code'), findsOneWidget);
    expect(find.text('A 6-digit code has been sent to your phone.'), findsOneWidget);
    expect(find.text('Submit'), findsOneWidget);
    expect(find.text('Resend in 0:30'), findsOneWidget);
    for (var n = 1; n <= 6; n++) {
      expect(digit(n), findsOneWidget);
      expect(digitText(tester, n), isEmpty);
    }
    expect(find.text('0'), findsNothing);
    expect(hasFocus(tester, 1), isTrue);
  });

  testWidgets('each box is labelled "Digit N of 6"', (tester) async {
    final semantics = tester.ensureSemantics();
    await pumpApp(tester);
    for (var n = 1; n <= 6; n++) {
      expect(find.bySemanticsLabel('Digit $n of 6'), findsOneWidget);
    }
    semantics.dispose();
  });

  testWidgets('typing advances focus and enables Submit when full', (tester) async {
    await pumpApp(tester);

    await tester.enterText(digit(1), '1');
    await tester.pump();
    expect(hasFocus(tester, 2), isTrue);
    expect(submitEnabled(tester), isFalse);

    for (var n = 2; n <= 6; n++) {
      await tester.enterText(digit(n), '$n');
      await tester.pump();
    }
    expect(submitEnabled(tester), isTrue);
  });

  testWidgets('backspace on an empty box clears the previous one', (tester) async {
    await pumpApp(tester);

    await tester.enterText(digit(1), '7');
    await tester.enterText(digit(2), '8');
    await tester.pump();
    expect(hasFocus(tester, 3), isTrue);

    await tester.sendKeyEvent(LogicalKeyboardKey.backspace);
    await tester.pump();
    expect(hasFocus(tester, 2), isTrue);
    expect(digitText(tester, 2), isEmpty);
    expect(digitText(tester, 1), '7');
  });

  testWidgets('pasting "Your code is 482913" fills all boxes', (tester) async {
    await pumpApp(tester);

    await tester.enterText(digit(1), 'Your code is 482913');
    await tester.pump();
    final code = [for (var n = 1; n <= 6; n++) digitText(tester, n)].join();
    expect(code, '482913');
  });

  testWidgets('123456 shows the success message', (tester) async {
    await pumpApp(tester);

    await tester.enterText(digit(1), '123456');
    await tester.pump();
    await tester.tap(find.byKey(const Key('otp_submit')));
    await tester.pump();
    expect(find.text("You're in."), findsOneWidget);
  });

  testWidgets('a wrong code shows the error, clears and refocuses box 1', (tester) async {
    await pumpApp(tester);

    await tester.enterText(digit(1), '000000');
    await tester.pump();
    await tester.tap(find.byKey(const Key('otp_submit')));
    await tester.pump();

    expect(find.text("That code didn't work. Try again."), findsOneWidget);
    for (var n = 1; n <= 6; n++) {
      expect(digitText(tester, n), isEmpty);
    }
    expect(hasFocus(tester, 1), isTrue);
  });

  testWidgets('resend unlocks after 30 s and restarts', (tester) async {
    await pumpApp(tester);

    expect(find.text('Resend in 0:30'), findsOneWidget);
    await tester.pump(const Duration(seconds: 21));
    expect(find.text('Resend in 0:09'), findsOneWidget);

    await tester.pump(const Duration(seconds: 9));
    expect(find.text("Didn't receive the code? "), findsOneWidget);
    await tester.tap(find.byKey(const Key('otp_resend')));
    await tester.pump();
    expect(find.text('Resend in 0:30'), findsOneWidget);
  });

  testWidgets('renders in dark mode', (tester) async {
    tester.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
    addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
    await pumpApp(tester);

    final context = tester.element(find.text('Enter your code'));
    expect(Theme.of(context).brightness, Brightness.dark);
  });
}
