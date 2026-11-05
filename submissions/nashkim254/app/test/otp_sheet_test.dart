import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/main.dart';
import 'package:hakka/otp/otp_strings.dart';
import 'package:hakka/theme/otp_colors.dart';

Finder digit(int position) => find.byKey(ValueKey(OtpTestIds.digit(position)));
final submit = find.byKey(const ValueKey(OtpTestIds.submit));
final resend = find.byKey(const ValueKey(OtpTestIds.resend));

String fieldText(int position) =>
    (digit(position).evaluate().single.widget as TextField).controller!.text;

List<String> allDigits() => [for (var i = 1; i <= 6; i++) fieldText(i)];

bool hasFocus(int position) =>
    (digit(position).evaluate().single.widget as TextField).focusNode!.hasFocus;

bool submitEnabled(WidgetTester tester) =>
    tester.widget<FilledButton>(submit).onPressed != null;

Future<void> pumpApp(WidgetTester tester) async {
  tester.view.physicalSize = const Size(1080, 2340);
  tester.view.devicePixelRatio = 3;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(const HakkaApp());
  await tester.pump();
}

/// Types into whichever box has focus, like a keyboard would.
Future<void> typeDigits(WidgetTester tester, String digits) async {
  for (final char in digits.split('')) {
    final focused = [for (var i = 1; i <= 6; i++) i].firstWhere(hasFocus);
    await tester.enterText(digit(focused), char);
    await tester.pump();
  }
}

void main() {
  testWidgets('shows the exact copy and six boxes', (tester) async {
    await pumpApp(tester);
    expect(find.text('Enter your code'), findsOneWidget);
    expect(
      find.text('A 6-digit code has been sent to your phone.'),
      findsOneWidget,
    );
    expect(find.text('Submit'), findsOneWidget);
    expect(find.text('Resend in 0:30'), findsOneWidget);
    for (var i = 1; i <= 6; i++) {
      expect(digit(i), findsOneWidget);
    }
    expect(find.byKey(const ValueKey(OtpTestIds.status)), findsOneWidget);
  });

  testWidgets('first box is focused and uses the numeric keyboard', (
    tester,
  ) async {
    await pumpApp(tester);
    expect(hasFocus(1), isTrue);
    final field = tester.widget<TextField>(digit(1));
    expect(field.keyboardType, TextInputType.number);
  });

  testWidgets('empty boxes show a placeholder, never 0', (tester) async {
    await pumpApp(tester);
    expect(find.text('0'), findsNothing);
    expect(find.text(OtpStrings.placeholder), findsNWidgets(5));
  });

  testWidgets('rejects non-digits', (tester) async {
    await pumpApp(tester);
    await tester.enterText(digit(1), 'a');
    await tester.pump();
    expect(fieldText(1), '');
    expect(hasFocus(1), isTrue);
  });

  testWidgets('typing moves focus to the next box', (tester) async {
    await pumpApp(tester);
    await typeDigits(tester, '21');
    expect(allDigits(), ['2', '1', '', '', '', '']);
    expect(hasFocus(3), isTrue);
  });

  testWidgets('backspace on an empty box clears the previous digit', (
    tester,
  ) async {
    await pumpApp(tester);
    await typeDigits(tester, '21');
    await tester.sendKeyEvent(LogicalKeyboardKey.backspace);
    await tester.pump();
    expect(allDigits(), ['2', '', '', '', '', '']);
    expect(hasFocus(2), isTrue);
  });

  testWidgets('pasting "Your code is 482913" fills all six boxes', (
    tester,
  ) async {
    await pumpApp(tester);
    await tester.enterText(digit(1), 'Your code is 482913');
    await tester.pump();
    expect(allDigits(), ['4', '8', '2', '9', '1', '3']);
  });

  testWidgets('Submit is disabled until all six digits are filled', (
    tester,
  ) async {
    await pumpApp(tester);
    expect(submitEnabled(tester), isFalse);
    await typeDigits(tester, '12345');
    expect(submitEnabled(tester), isFalse);
    await typeDigits(tester, '6');
    expect(submitEnabled(tester), isTrue);
  });

  testWidgets('123456 shows the success message', (tester) async {
    await pumpApp(tester);
    await typeDigits(tester, '123456');
    await tester.tap(submit);
    await tester.pump();
    expect(find.text("You're in."), findsOneWidget);
  });

  testWidgets('a wrong code shows the error, clears and refocuses box 1', (
    tester,
  ) async {
    await pumpApp(tester);
    await typeDigits(tester, '111111');
    await tester.tap(submit);
    await tester.pump();
    expect(find.text("That code didn't work. Try again."), findsOneWidget);
    expect(allDigits(), everyElement(''));
    expect(hasFocus(1), isTrue);
    expect(submitEnabled(tester), isFalse);
  });

  testWidgets('after a wrong code, the right code still succeeds', (
    tester,
  ) async {
    await pumpApp(tester);
    await typeDigits(tester, '111111');
    await tester.tap(submit);
    await tester.pump();
    await typeDigits(tester, '123456');
    expect(allDigits(), ['1', '2', '3', '4', '5', '6']);
    expect(find.text("That code didn't work. Try again."), findsNothing);
    await tester.tap(submit);
    await tester.pump();
    expect(find.text("You're in."), findsOneWidget);
  });

  testWidgets('typing over a filled box replaces its digit', (tester) async {
    await pumpApp(tester);
    await typeDigits(tester, '12');
    await tester.tap(digit(1));
    await tester.pump();
    expect(hasFocus(1), isTrue);
    await tester.enterText(digit(1), '9');
    await tester.pump();
    expect(allDigits().take(2), ['9', '2']);
    expect(hasFocus(2), isTrue);
  });

  testWidgets('Resend counts down 30 s, then restarts when tapped', (
    tester,
  ) async {
    await pumpApp(tester);
    expect(tester.widget<TextButton>(resend).onPressed, isNull);
    await tester.pump(const Duration(seconds: 21));
    expect(find.text('Resend in 0:09'), findsOneWidget);
    await tester.pump(const Duration(seconds: 9));
    expect(find.text("Didn't receive the code? "), findsOneWidget);
    expect(find.text('Resend'), findsOneWidget);
    expect(tester.widget<TextButton>(resend).onPressed, isNotNull);
    await tester.tap(resend);
    await tester.pump();
    expect(find.text('Resend in 0:30'), findsOneWidget);
    expect(tester.widget<TextButton>(resend).onPressed, isNull);
  });

  testWidgets('each box is announced as "Digit N of 6"', (tester) async {
    final semantics = tester.ensureSemantics();
    await pumpApp(tester);
    for (var i = 1; i <= 6; i++) {
      expect(find.bySemanticsLabel(RegExp('^Digit $i of 6')), findsOneWidget);
    }
    semantics.dispose();
  });

  testWidgets('uses the dark palette in dark mode', (tester) async {
    tester.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
    addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
    await pumpApp(tester);
    final context = tester.element(submit);
    expect(OtpColors.of(context), same(OtpColors.dark));
  });
}
