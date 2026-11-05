import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/otp_colors.dart';
import 'package:hakka/otp_screen.dart';

final field = find.byKey(const Key('otp_field'));
final submit = find.byKey(const Key('otp_submit'));
final resend = find.byKey(const Key('otp_resend'));
final status = find.byKey(const Key('otp_status'));
Finder digit(int n) => find.byKey(Key('otp_digit_$n'));

Future<void> pumpOtp(WidgetTester tester, {ThemeMode mode = ThemeMode.light}) async {
  await tester.pumpWidget(MaterialApp(
    theme: ThemeData(brightness: Brightness.light),
    darkTheme: ThemeData(brightness: Brightness.dark),
    themeMode: mode,
    home: const OtpScreen(),
  ));
  await tester.pump();
}

String boxText(WidgetTester tester, int n) =>
    tester.widget<Text>(find.descendant(of: digit(n), matching: find.byType(Text))).data!;

BoxDecoration boxDecoration(WidgetTester tester, int n) =>
    tester.widget<AnimatedContainer>(
      find.descendant(of: digit(n), matching: find.byType(AnimatedContainer)),
    ).decoration! as BoxDecoration;

String code(WidgetTester tester) => [for (var n = 1; n <= 6; n++) boxText(tester, n)].join();

bool submitEnabled(WidgetTester tester) => tester.widget<FilledButton>(submit).onPressed != null;
bool resendEnabled(WidgetTester tester) => tester.widget<TextButton>(resend).onPressed != null;

void main() {
  testWidgets('AC1: six circular boxes, digits only, numeric keyboard autofocused', (tester) async {
    await pumpOtp(tester);

    for (var n = 1; n <= 6; n++) {
      expect(digit(n), findsOneWidget);
      expect(boxDecoration(tester, n).shape, BoxShape.circle);
    }
    final textField = tester.widget<TextField>(field);
    expect(textField.keyboardType, TextInputType.number);
    expect(textField.autofocus, isTrue);
    expect(tester.testTextInput.isVisible, isTrue);
    expect(tester.getSemantics(digit(1)), isSemantics(isFocused: true));

    await tester.enterText(field, '1a2-b');
    await tester.pump();
    expect(code(tester), '12––––');
  });

  testWidgets('AC2: filled, focused and empty states with placeholder', (tester) async {
    await pumpOtp(tester);
    await tester.enterText(field, '21');
    await tester.pump();

    const c = OtpColors.light;
    expect(boxText(tester, 1), '2');
    expect(boxDecoration(tester, 1).color, c.digitFilledBg);
    expect(tester.widget<Text>(find.descendant(of: digit(1), matching: find.byType(Text))).style!.color,
        c.digitFilledText);

    expect(boxDecoration(tester, 3).color, c.digitFocusedBg);
    expect(boxDecoration(tester, 3).border!.top.color, c.digitFocusedOutline);

    expect(boxDecoration(tester, 4).color, c.digitEmptyBg);
    expect(boxText(tester, 4), '–');
    expect(find.descendant(of: digit(4), matching: find.text('0')), findsNothing);
  });

  testWidgets('AC3: typing a digit moves focus to the next box', (tester) async {
    await pumpOtp(tester);
    await tester.enterText(field, '7');
    await tester.pump();

    expect(boxText(tester, 1), '7');
    expect(tester.getSemantics(digit(2)), isSemantics(isFocused: true));
    expect(tester.getSemantics(digit(1)), isSemantics(isFocused: false));
  });

  testWidgets('AC4: backspace on an empty box clears the previous digit', (tester) async {
    await pumpOtp(tester);
    await tester.enterText(field, '123');
    await tester.pump();
    expect(tester.getSemantics(digit(4)), isSemantics(isFocused: true));

    await tester.enterText(field, '12'); // backspace
    await tester.pump();

    expect(code(tester), '12––––');
    expect(tester.getSemantics(digit(3)), isSemantics(isFocused: true));
  });

  testWidgets('AC5: pasting "Your code is 482913" fills 482913', (tester) async {
    await pumpOtp(tester);
    await tester.enterText(field, 'Your code is 482913');
    await tester.pump();
    expect(code(tester), '482913');

    // Pasting over a partial code replaces it too.
    await tester.enterText(field, '12');
    await tester.enterText(field, '12Your code is 482913');
    await tester.pump();
    expect(code(tester), '482913');
  });

  testWidgets('AC6: Submit disabled until all six digits are filled', (tester) async {
    await pumpOtp(tester);
    for (final partial in ['', '1', '12345']) {
      await tester.enterText(field, partial);
      await tester.pump();
      expect(submitEnabled(tester), isFalse, reason: 'with "$partial"');
    }
    await tester.enterText(field, '123456');
    await tester.pump();
    expect(submitEnabled(tester), isTrue);
  });

  testWidgets('AC7: 123456 + Submit shows the success message', (tester) async {
    await pumpOtp(tester);
    await tester.enterText(field, '123456');
    await tester.pump();
    await tester.tap(submit);
    await tester.pump();

    expect(tester.widget<Text>(status).data, "You're in.");
  });

  testWidgets('AC8: wrong code shows error, clears boxes, refocuses box 1', (tester) async {
    await pumpOtp(tester);
    await tester.enterText(field, '654321');
    await tester.pump();
    await tester.tap(submit);
    await tester.pump();

    expect(tester.widget<Text>(status).data, "That code didn't work. Try again.");
    expect(code(tester), '––––––');
    expect(tester.getSemantics(digit(1)), isSemantics(isFocused: true));
    expect(submitEnabled(tester), isFalse);
  });

  testWidgets('AC9: Resend counts down 30 s, then tapping restarts it', (tester) async {
    await pumpOtp(tester);
    expect(find.text('Resend in 0:30'), findsOneWidget);
    expect(resendEnabled(tester), isFalse);

    await tester.pump(const Duration(seconds: 1));
    expect(find.text('Resend in 0:29'), findsOneWidget);

    await tester.pump(const Duration(seconds: 21));
    expect(find.text('Resend in 0:08'), findsOneWidget);

    await tester.pump(const Duration(seconds: 8));
    expect(find.text('Resend'), findsOneWidget);
    expect(find.text("Didn't receive the code? "), findsOneWidget);
    expect(resendEnabled(tester), isTrue);

    await tester.tap(resend);
    await tester.pump();
    expect(find.text('Resend in 0:30'), findsOneWidget);
    expect(resendEnabled(tester), isFalse);
  });

  testWidgets('AC10: boxes announced as "Digit N of 6"', (tester) async {
    await pumpOtp(tester);
    for (var n = 1; n <= 6; n++) {
      expect(find.bySemanticsLabel('Digit $n of 6'), findsOneWidget);
    }
  });

  testWidgets('AC10: dark mode uses the dark palette', (tester) async {
    await pumpOtp(tester, mode: ThemeMode.dark);
    await tester.enterText(field, '5');
    await tester.pump();

    const c = OtpColors.dark;
    expect(boxDecoration(tester, 1).color, c.digitFilledBg);
    expect(boxDecoration(tester, 2).border!.top.color, c.digitFocusedOutline);
    expect(boxDecoration(tester, 3).color, c.digitEmptyBg);
  });

  testWidgets('shows the exact copy, logo and test IDs', (tester) async {
    await pumpOtp(tester);
    expect(find.text('Enter your code'), findsOneWidget);
    expect(find.text('A 6-digit code has been sent to your phone.'), findsOneWidget);
    expect(find.text('Submit'), findsOneWidget);
    expect(find.bySemanticsLabel('Thamani logo'), findsOneWidget);
    for (final f in [submit, resend, status]) {
      expect(f, findsOneWidget);
    }
  });
}
