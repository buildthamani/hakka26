import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/main.dart';
import 'package:hakka/otp/palette.dart';

late OtpPalettes testPalettes;
late OtpColors light;
late OtpColors dark;

Future<void> pumpOtp(
  WidgetTester tester, {
  Brightness brightness = Brightness.light,
}) async {
  tester.view.physicalSize = const Size(390, 844);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(() async {
    await tester.pumpWidget(const SizedBox.shrink());
    tester.view.resetPhysicalSize();
    tester.view.resetDevicePixelRatio();
  });

  await tester.pumpWidget(
    HakkaApp(
      palettes: testPalettes,
      themeMode:
          brightness == Brightness.dark ? ThemeMode.dark : ThemeMode.light,
    ),
  );
  await tester.pump(const Duration(milliseconds: 20));
}

Future<void> enterDigits(WidgetTester tester, String value) async {
  await tester.enterText(find.byKey(const Key('otp_input')), value);
  await tester.pump(const Duration(milliseconds: 20));
}

FilledButton submitButton(WidgetTester tester) =>
    tester.widget<FilledButton>(find.byKey(const Key('otp_submit')));

TextButton resendButton(WidgetTester tester) =>
    tester.widget<TextButton>(find.byKey(const Key('otp_resend')));

BoxDecoration boxDecoration(WidgetTester tester, int index) {
  final box = tester.widget<AnimatedContainer>(
    find.byKey(Key('otp_box_$index')),
  );
  return box.decoration! as BoxDecoration;
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUpAll(() async {
    testPalettes = await OtpPalettes.load();
    light = testPalettes.light;
    dark = testPalettes.dark;
  });

  test('the colour palette asset is bundled and parses', () {
    expect(light.digitFilledBg, const Color(0xFF12361B));
    expect(light.digitFocusedOutline, const Color(0xFF111111));
    expect(dark.sheet, const Color(0xFF161A17));
    expect(dark.buttonBg, const Color(0xFF2E7D45));
  });

  testWidgets('shows the required copy, logo and six 48dp boxes', (
    tester,
  ) async {
    await pumpOtp(tester);

    expect(find.text('Enter your code'), findsOneWidget);
    expect(
      find.text('A 6-digit code has been sent to your phone.'),
      findsOneWidget,
    );
    expect(find.text('Submit'), findsOneWidget);
    expect(find.text("Didn't receive the code? "), findsOneWidget);
    expect(find.text('Resend in 0:30'), findsOneWidget);

    for (var i = 1; i <= 6; i++) {
      expect(find.byKey(Key('otp_digit_$i')), findsOneWidget);
      expect(
        tester.getSize(find.byKey(Key('otp_digit_$i'))),
        const Size(48, 48),
      );
      expect(boxDecoration(tester, i).shape, BoxShape.circle);
    }

    final logo = tester.widget<Image>(find.byType(Image));
    expect((logo.image as AssetImage).assetName, 'assets/thamani-logo.png');
  });

  testWidgets('autofocuses a numeric keyboard and rejects non-digits', (
    tester,
  ) async {
    await pumpOtp(tester);

    final field = tester.widget<TextField>(find.byKey(const Key('otp_input')));
    expect(field.keyboardType, TextInputType.number);
    expect(field.autofocus, isTrue);
    expect(field.focusNode!.hasFocus, isTrue);

    await enterDigits(tester, 'a1b2c');
    expect(find.text('1'), findsOneWidget);
    expect(find.text('2'), findsOneWidget);
    expect(find.text('a'), findsNothing);
    expect(find.text('b'), findsNothing);
  });

  testWidgets('typing advances the focused box', (tester) async {
    await pumpOtp(tester);
    await enterDigits(tester, '4');

    expect(boxDecoration(tester, 1).color, light.digitFilledBg);
    expect(boxDecoration(tester, 2).color, light.digitFocusedBg);
    expect(
      (boxDecoration(tester, 2).border! as Border).top.color,
      light.digitFocusedOutline,
    );
    expect(boxDecoration(tester, 3).color, light.digitEmptyBg);
  });

  testWidgets('backspace on the focused empty box clears the previous digit', (
    tester,
  ) async {
    await pumpOtp(tester);
    await enterDigits(tester, '48');
    expect(
      find.descendant(
        of: find.byKey(const Key('otp_digit_2')),
        matching: find.text('8'),
      ),
      findsOneWidget,
    );

    await tester.sendKeyEvent(LogicalKeyboardKey.backspace);
    await tester.pump(const Duration(milliseconds: 20));

    expect(
      find.descendant(
        of: find.byKey(const Key('otp_digit_2')),
        matching: find.text('8'),
      ),
      findsNothing,
    );
    expect(
      find.descendant(
        of: find.byKey(const Key('otp_digit_1')),
        matching: find.text('4'),
      ),
      findsOneWidget,
    );
    expect(boxDecoration(tester, 2).color, light.digitFocusedBg);
  });

  testWidgets('pasting an SMS code fills all six boxes', (tester) async {
    await pumpOtp(tester);
    await enterDigits(tester, 'Your code is 482913');

    for (final digit in ['4', '8', '2', '9', '1', '3']) {
      expect(find.text(digit), findsOneWidget);
    }
    expect(find.text('-'), findsNothing);
    expect(submitButton(tester).onPressed, isNotNull);
  });

  testWidgets('submit stays disabled until all six digits are entered', (
    tester,
  ) async {
    await pumpOtp(tester);
    expect(submitButton(tester).onPressed, isNull);

    await enterDigits(tester, '12345');
    expect(submitButton(tester).onPressed, isNull);

    await enterDigits(tester, '123456');
    expect(submitButton(tester).onPressed, isNotNull);
  });

  testWidgets('123456 shows the success message', (tester) async {
    await pumpOtp(tester);
    await enterDigits(tester, '123456');
    await tester.tap(find.byKey(const Key('otp_submit')));
    await tester.pump(const Duration(milliseconds: 50));

    expect(find.text("You're in."), findsOneWidget);
    expect(find.text("That code didn't work. Try again."), findsNothing);
  });

  testWidgets('a wrong code shows the error, clears the boxes and focuses the '
      'first', (tester) async {
    await pumpOtp(tester);
    await enterDigits(tester, '000000');
    await tester.tap(find.byKey(const Key('otp_submit')));
    await tester.pump(const Duration(milliseconds: 50));

    expect(find.text("That code didn't work. Try again."), findsOneWidget);
    expect(find.text('-'), findsNWidgets(6));

    final field = tester.widget<TextField>(find.byKey(const Key('otp_input')));
    expect(field.controller!.text, isEmpty);
    expect(field.focusNode!.hasFocus, isTrue);
    expect(boxDecoration(tester, 1).color, light.digitFocusedBg);
  });

  testWidgets('resend is disabled for 30 seconds and restarts when tapped', (
    tester,
  ) async {
    await pumpOtp(tester);

    expect(find.text('Resend in 0:30'), findsOneWidget);
    expect(resendButton(tester).onPressed, isNull);

    await tester.pump(const Duration(seconds: 29));
    expect(find.text('Resend in 0:01'), findsOneWidget);
    expect(resendButton(tester).onPressed, isNull);

    await tester.pump(const Duration(seconds: 1));
    expect(find.text('Resend'), findsOneWidget);
    expect(resendButton(tester).onPressed, isNotNull);

    await tester.tap(find.byKey(const Key('otp_resend')));
    await tester.pump(const Duration(milliseconds: 20));
    expect(find.text('Resend in 0:30'), findsOneWidget);
    expect(resendButton(tester).onPressed, isNull);
  });

  testWidgets('announces each box as Digit N of 6', (tester) async {
    final handle = tester.ensureSemantics();
    await pumpOtp(tester);

    for (var i = 1; i <= 6; i++) {
      final data = tester
          .getSemantics(find.byKey(Key('otp_digit_$i')))
          .getSemanticsData();
      expect(data.label, 'Digit $i of 6');
      expect(data.value, 'empty');
    }

    await enterDigits(tester, '4');
    final first = tester
        .getSemantics(find.byKey(const Key('otp_digit_1')))
        .getSemanticsData();
    expect(first.value, '4');
    handle.dispose();
  });

  testWidgets('supports dark mode', (tester) async {
    await pumpOtp(tester, brightness: Brightness.dark);

    final logo = tester.widget<Image>(find.byType(Image));
    expect(
      (logo.image as AssetImage).assetName,
      'assets/thamani-logo-white.png',
    );

    final sheet = tester.widget<Container>(
      find.byKey(const Key('otp_sheet')),
    );
    expect((sheet.decoration! as BoxDecoration).color, dark.sheet);
    expect(boxDecoration(tester, 1).color, dark.digitFocusedBg);
    expect(boxDecoration(tester, 2).color, dark.digitEmptyBg);

    final title = tester.widget<Text>(find.text('Enter your code'));
    expect(title.style!.color, dark.textPrimary);
  });
}
