import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/main.dart';
import 'package:hakka/otp/otp_colours.dart';
import 'package:hakka/otp/otp_controller.dart';
import 'package:hakka/otp/otp_copy.dart';
import 'package:hakka/otp/otp_screen.dart';

const _sentinel = OtpController.fieldSentinel;

Finder digitBox(int n) => find.byKey(ValueKey('otp_digit_$n'));
final submitButton = find.byKey(const ValueKey('otp_submit'));
final resendButton = find.byKey(const ValueKey('otp_resend'));
final statusText = find.byKey(const ValueKey('otp_status'));

Future<void> pumpOtp(
  WidgetTester tester, {
  Brightness brightness = Brightness.light,
}) async {
  tester.platformDispatcher.platformBrightnessTestValue = brightness;
  addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
  await tester.pumpWidget(const HakkaApp());
  await tester.pump();
}

TextField field(WidgetTester tester, int n) =>
    tester.widget<TextField>(digitBox(n));

String digitIn(WidgetTester tester, int n) =>
    field(tester, n).controller!.text.replaceAll(_sentinel, '');

String allDigits(WidgetTester tester) =>
    [for (var n = 1; n <= 6; n++) digitIn(tester, n)].join();

bool hasFocus(WidgetTester tester, int n) =>
    field(tester, n).focusNode!.hasFocus;

/// Types [text] at the end of box [n], like a keyboard or a paste would.
Future<void> typeInto(WidgetTester tester, int n, String text) async {
  await tester.enterText(
    digitBox(n),
    '${field(tester, n).controller!.text}$text',
  );
  await tester.pump(const Duration(milliseconds: 200));
}

BoxDecoration boxDecoration(WidgetTester tester, int n) {
  final container = tester.widget<AnimatedContainer>(
    find.ancestor(of: digitBox(n), matching: find.byType(AnimatedContainer)),
  );
  return container.decoration! as BoxDecoration;
}

bool submitEnabled(WidgetTester tester) =>
    tester.widget<FilledButton>(submitButton).onPressed != null;

String statusMessage(WidgetTester tester) =>
    tester.widget<Text>(statusText).data!;

void main() {
  testWidgets('shows the issue copy, logo and full-width Submit', (
    tester,
  ) async {
    await pumpOtp(tester);

    expect(find.text(OtpCopy.title), findsOneWidget);
    expect(find.text('Enter your code'), findsOneWidget);
    expect(
      find.text('A 6-digit code has been sent to your phone.'),
      findsOneWidget,
    );
    expect(find.text("Didn't receive the code? "), findsOneWidget);
    expect(find.text('Submit'), findsOneWidget);
    expect(find.bySemanticsLabel('Thamani logo'), findsOneWidget);
    expect(
      tester.getSize(submitButton).width,
      tester.getSize(find.byKey(const ValueKey('otp_sheet'))).width - 48,
    );
  });

  testWidgets('AC1: six circular boxes, digits only, numeric keyboard opens', (
    tester,
  ) async {
    await pumpOtp(tester);

    for (var n = 1; n <= 6; n++) {
      expect(digitBox(n), findsOneWidget);
      expect(boxDecoration(tester, n).shape, BoxShape.circle);
      expect(field(tester, n).keyboardType, TextInputType.number);
    }
    expect(hasFocus(tester, 1), isTrue);
    expect(tester.testTextInput.isVisible, isTrue);

    await typeInto(tester, 1, 'a');
    expect(digitIn(tester, 1), isEmpty);
    expect(hasFocus(tester, 1), isTrue);
  });

  testWidgets('AC2: filled, focused and empty box states', (tester) async {
    await pumpOtp(tester);
    await typeInto(tester, 1, '2');
    await typeInto(tester, 2, '1');

    final filled = boxDecoration(tester, 1);
    expect(filled.color, OtpColours.light.digitFilledBg);
    final filledText = field(tester, 1).style!;
    expect(filledText.color, OtpColours.light.digitFilledText);

    final focused = boxDecoration(tester, 3);
    expect(hasFocus(tester, 3), isTrue);
    expect(focused.color, OtpColours.light.digitFocusedBg);
    expect(focused.border!.top.color, OtpColours.light.digitFocusedOutline);

    final empty = boxDecoration(tester, 4);
    expect(empty.color, OtpColours.light.digitEmptyBg);
    expect(find.text(OtpCopy.placeholder), findsNWidgets(3));
    expect(OtpCopy.placeholder, isNot('0'));
    expect(find.text('0'), findsNothing);
  });

  testWidgets('AC3: typing a digit moves focus to the next box', (
    tester,
  ) async {
    await pumpOtp(tester);

    for (var n = 1; n <= 5; n++) {
      await typeInto(tester, n, '$n');
      expect(hasFocus(tester, n + 1), isTrue, reason: 'after box $n');
    }
    expect(allDigits(tester), '12345');
  });

  testWidgets('AC4: backspace on an empty box clears the previous digit', (
    tester,
  ) async {
    await pumpOtp(tester);
    await typeInto(tester, 1, '4');
    await typeInto(tester, 2, '8');
    expect(hasFocus(tester, 3), isTrue);

    await tester.sendKeyEvent(LogicalKeyboardKey.backspace);
    await tester.pump();

    expect(allDigits(tester), '4');
    expect(hasFocus(tester, 2), isTrue);

    await tester.sendKeyEvent(LogicalKeyboardKey.backspace);
    await tester.pump();
    expect(allDigits(tester), '');
    expect(hasFocus(tester, 1), isTrue);
  });

  testWidgets('AC5: pasting "Your code is 482913" fills all six boxes', (
    tester,
  ) async {
    await pumpOtp(tester);

    await typeInto(tester, 1, 'Your code is 482913');

    expect(allDigits(tester), '482913');
    expect(hasFocus(tester, 6), isTrue);
  });

  testWidgets('AC6: Submit is disabled until all six digits are filled', (
    tester,
  ) async {
    await pumpOtp(tester);
    expect(submitEnabled(tester), isFalse);

    for (var n = 1; n <= 5; n++) {
      await typeInto(tester, n, '9');
      expect(submitEnabled(tester), isFalse);
    }
    await typeInto(tester, 6, '9');
    expect(submitEnabled(tester), isTrue);
  });

  testWidgets('AC7: 123456 + Submit shows the success message', (tester) async {
    await pumpOtp(tester);
    expect(statusMessage(tester), isEmpty);

    await typeInto(tester, 1, '123456');
    await tester.tap(submitButton);
    await tester.pump();

    expect(statusMessage(tester), "You're in.");
  });

  testWidgets(
    'AC8: a wrong code shows the error, clears boxes, refocuses box 1',
    (tester) async {
      await pumpOtp(tester);

      await typeInto(tester, 1, '654321');
      await tester.tap(submitButton);
      await tester.pump();

      expect(statusMessage(tester), "That code didn't work. Try again.");
      expect(allDigits(tester), isEmpty);
      expect(hasFocus(tester, 1), isTrue);
      expect(submitEnabled(tester), isFalse);
    },
  );

  testWidgets(
    'AC9: Resend is disabled for a real 30 s countdown, then restarts',
    (tester) async {
      await pumpOtp(tester);
      bool resendEnabled() =>
          tester.widget<TextButton>(resendButton).onPressed != null;

      expect(find.text('Resend in 0:30'), findsOneWidget);
      expect(resendEnabled(), isFalse);

      await tester.pump(const Duration(seconds: 1));
      expect(find.text('Resend in 0:29'), findsOneWidget);

      await tester.pump(const Duration(seconds: 20));
      expect(find.text('Resend in 0:09'), findsOneWidget);
      expect(resendEnabled(), isFalse);

      await tester.pump(const Duration(seconds: 9));
      expect(find.text('Resend'), findsOneWidget);
      expect(resendEnabled(), isTrue);

      await tester.tap(resendButton);
      await tester.pump();
      expect(find.text('Resend in 0:30'), findsOneWidget);
      expect(resendEnabled(), isFalse);
    },
  );

  testWidgets('AC10: boxes are announced as "Digit N of 6"', (tester) async {
    final semantics = tester.ensureSemantics();
    await pumpOtp(tester);

    for (var n = 1; n <= 6; n++) {
      expect(find.bySemanticsLabel('Digit $n of 6'), findsOneWidget);
    }
    semantics.dispose();
  });

  testWidgets('AC10: dark mode uses the dark palette', (tester) async {
    await pumpOtp(tester, brightness: Brightness.dark);

    final sheet = tester.widget<Material>(
      find.byKey(const ValueKey('otp_sheet')),
    );
    expect(sheet.color, OtpColours.dark.sheet);
    expect(boxDecoration(tester, 1).color, OtpColours.dark.digitFocusedBg);
    expect(boxDecoration(tester, 2).color, OtpColours.dark.digitEmptyBg);
    final submit = tester.widget<FilledButton>(submitButton);
    expect(
      submit.style!.backgroundColor!.resolve({WidgetState.disabled}),
      OtpColours.dark.buttonDisabledBg,
    );
  });

  testWidgets('verification goes through the injected verifier', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(home: OtpScreen(verifier: (code) => code == '000111')),
    );
    await tester.pump();

    await typeInto(tester, 1, '000111');
    await tester.tap(submitButton);
    await tester.pump();

    expect(statusMessage(tester), OtpCopy.success);
  });
}
