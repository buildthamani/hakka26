import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/main.dart';
import 'package:hakka/otp/otp_colors.dart';
import 'package:hakka/otp/otp_input.dart';
import 'package:hakka/otp/otp_screen.dart';

final _input = find.byKey(const Key('otp_input'));
final _submit = find.byKey(const Key('otp_submit'));
final _resend = find.byKey(const Key('otp_resend'));

Finder _digit(int n) => find.byKey(Key('otp_digit_$n'));

Future<void> _pumpApp(WidgetTester tester, {ThemeMode themeMode = ThemeMode.system}) async {
  tester.view.physicalSize = const Size(1170, 2532);
  tester.view.devicePixelRatio = 3;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(HakkaApp(themeMode: themeMode));
  await tester.pump();
}

List<OtpBoxState> _states(WidgetTester tester) =>
    [for (var n = 1; n <= 6; n++) tester.widget<OtpDigitBox>(_digit(n)).state];

List<String> _digits(WidgetTester tester) => [for (var n = 1; n <= 6; n++) tester.widget<OtpDigitBox>(_digit(n)).digit];

bool _submitEnabled(WidgetTester tester) => tester.widget<FilledButton>(_submit).onPressed != null;

const _f = OtpBoxState.filled, _o = OtpBoxState.focused, _e = OtpBoxState.empty;

void main() {
  testWidgets('shows the exact copy, logo and all test IDs', (tester) async {
    await _pumpApp(tester);

    expect(find.text(OtpCopy.title), findsOneWidget);
    expect(find.text('Enter your code'), findsOneWidget);
    expect(find.text('A 6-digit code has been sent to your phone.'), findsOneWidget);
    expect(find.text("Didn't receive the code? "), findsOneWidget);
    expect(find.text('Submit'), findsOneWidget);
    expect(find.bySemanticsLabel('Thamani logo'), findsOneWidget);
    for (var n = 1; n <= 6; n++) {
      expect(_digit(n), findsOneWidget);
    }
    expect(_submit, findsOneWidget);
    expect(_resend, findsOneWidget);
    expect(find.byKey(const Key('otp_status')), findsOneWidget);
  });

  testWidgets('AC1: six circular boxes, numeric keyboard opens automatically', (tester) async {
    await _pumpApp(tester);

    expect(find.byType(OtpDigitBox), findsNWidgets(6));
    final field = tester.widget<TextField>(_input);
    expect(field.keyboardType, TextInputType.number);
    expect(field.autofocus, isTrue);
    expect(field.focusNode!.hasFocus, isTrue);
    expect(tester.testTextInput.isVisible, isTrue);

    final circle = tester.widget<AnimatedContainer>(
      find.descendant(of: _digit(1), matching: find.byType(AnimatedContainer)),
    );
    expect((circle.decoration! as BoxDecoration).shape, BoxShape.circle);

    await tester.enterText(_input, '4a-8');
    await tester.pump();
    expect(_digits(tester), ['4', '8', '', '', '', '']);
  });

  testWidgets('AC2: filled, focused and empty states; placeholder is never 0', (tester) async {
    await _pumpApp(tester);
    expect(_states(tester), [_o, _e, _e, _e, _e, _e]);
    expect(find.text('0'), findsNothing);
    expect(find.text(OtpDigitBox.placeholder), findsNWidgets(6));

    await tester.enterText(_input, '21');
    await tester.pump();
    expect(_states(tester), [_f, _f, _o, _e, _e, _e]);
    expect(find.text('0'), findsNothing);

    final filled = tester.widget<AnimatedContainer>(
      find.descendant(of: _digit(1), matching: find.byType(AnimatedContainer)),
    );
    expect((filled.decoration! as BoxDecoration).color, OtpColors.light.digitFilledBg);
    final focused = tester.widget<AnimatedContainer>(
      find.descendant(of: _digit(3), matching: find.byType(AnimatedContainer)),
    );
    final focusedBorder = (focused.decoration! as BoxDecoration).border! as Border;
    expect((focused.decoration! as BoxDecoration).color, OtpColors.light.digitFocusedBg);
    expect(focusedBorder.top.color, OtpColors.light.digitFocusedOutline);
  });

  testWidgets('AC3: typing a digit moves focus to the next box', (tester) async {
    await _pumpApp(tester);
    await tester.enterText(_input, '7');
    await tester.pump();
    expect(_states(tester), [_f, _o, _e, _e, _e, _e]);
    await tester.enterText(_input, '73');
    await tester.pump();
    expect(_states(tester), [_f, _f, _o, _e, _e, _e]);
  });

  testWidgets('AC4: backspace on an empty box clears the previous digit', (tester) async {
    await _pumpApp(tester);
    await tester.enterText(_input, '215');
    await tester.pump();
    expect(_states(tester)[3], _o);

    await tester.enterText(_input, '21');
    await tester.pump();
    expect(_digits(tester), ['2', '1', '', '', '', '']);
    expect(_states(tester), [_f, _f, _o, _e, _e, _e]);
  });

  testWidgets('AC5: pasting "Your code is 482913" fills the boxes', (tester) async {
    await _pumpApp(tester);
    await tester.enterText(_input, 'Your code is 482913');
    await tester.pump();
    expect(_digits(tester), ['4', '8', '2', '9', '1', '3']);
  });

  testWidgets('AC5: long-press pastes from the clipboard', (tester) async {
    tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(SystemChannels.platform, (call) async {
      if (call.method == 'Clipboard.getData') return {'text': 'Your code is 482913'};
      return null;
    });
    addTearDown(() => tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(SystemChannels.platform, null));

    await _pumpApp(tester);
    await tester.longPress(_digit(1));
    await tester.pump();
    expect(_digits(tester), ['4', '8', '2', '9', '1', '3']);
  });

  testWidgets('AC6: Submit is disabled until all six digits are filled', (tester) async {
    await _pumpApp(tester);
    expect(_submitEnabled(tester), isFalse);
    await tester.enterText(_input, '12345');
    await tester.pump();
    expect(_submitEnabled(tester), isFalse);
    await tester.enterText(_input, '123456');
    await tester.pump();
    expect(_submitEnabled(tester), isTrue);
  });

  testWidgets('AC7: 123456 + Submit shows the success message', (tester) async {
    await _pumpApp(tester);
    await tester.enterText(_input, '123456');
    await tester.pump();
    await tester.tap(_submit);
    await tester.pump();
    expect(find.text("You're in."), findsOneWidget);
  });

  testWidgets('AC8: a wrong code shows the error, clears the boxes and refocuses box 1', (tester) async {
    await _pumpApp(tester);
    await tester.enterText(_input, '111111');
    await tester.pump();
    await tester.tap(_submit);
    await tester.pump();

    expect(find.text("That code didn't work. Try again."), findsOneWidget);
    expect(_digits(tester), everyElement(isEmpty));
    expect(tester.widget<TextField>(_input).focusNode!.hasFocus, isTrue);
    expect(_states(tester), [_o, _e, _e, _e, _e, _e]);
    expect(_submitEnabled(tester), isFalse);
  });

  testWidgets('AC9: Resend counts down 30 s, then restarts when tapped', (tester) async {
    await _pumpApp(tester);
    expect(find.text('Resend in 0:30'), findsOneWidget);
    expect(tester.widget<TextButton>(_resend).onPressed, isNull);

    await tester.pump(const Duration(seconds: 1));
    expect(find.text('Resend in 0:29'), findsOneWidget);

    await tester.pump(const Duration(seconds: 29));
    expect(find.text('Resend'), findsOneWidget);
    expect(tester.widget<TextButton>(_resend).onPressed, isNotNull);

    await tester.tap(_resend);
    await tester.pump();
    expect(find.text('Resend in 0:30'), findsOneWidget);
    expect(tester.widget<TextButton>(_resend).onPressed, isNull);
  });

  testWidgets('AC10: each box is announced as "Digit N of 6"', (tester) async {
    final semantics = tester.ensureSemantics();
    await _pumpApp(tester);
    for (var n = 1; n <= 6; n++) {
      expect(find.bySemanticsLabel('Digit $n of 6'), findsOneWidget);
    }
    semantics.dispose();
  });

  testWidgets('AC10: follows the system dark mode', (tester) async {
    tester.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
    addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
    await _pumpApp(tester);

    final context = tester.element(_digit(1));
    expect(Theme.of(context).brightness, Brightness.dark);
    expect(OtpColors.of(context), OtpColors.dark);
    expect(find.image(const AssetImage('assets/thamani-logo-white.png')), findsOneWidget);
  });
}
