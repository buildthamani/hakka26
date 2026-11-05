import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/main.dart';
import 'package:hakka/otp_screen.dart';
import 'package:hakka/otp_service.dart';
import 'package:hakka/otp_theme.dart';

const _lightColors = OtpColors(
  sheet: Color(0xFFFFFFFF),
  scrim: Color(0x99000000),
  textPrimary: Color(0xFF111111),
  textSecondary: Color(0xFF5F6368),
  digitFilledBg: Color(0xFF12361B),
  digitFilledText: Color(0xFFFFFFFF),
  digitFocusedBg: Color(0xFFFFFFFF),
  digitFocusedOutline: Color(0xFF111111),
  digitEmptyBg: Color(0xFFF1F1F1),
  digitEmptyOutline: Color(0xFFE0E0E0),
  digitPlaceholder: Color(0xFF9E9E9E),
  buttonBg: Color(0xFF12361B),
  buttonText: Color(0xFFFFFFFF),
  buttonDisabledBg: Color(0xFFC9D1CB),
  link: Color(0xFF12361B),
  success: Color(0xFF1B7F3A),
  error: Color(0xFFC62828),
);

const _darkColors = OtpColors(
  sheet: Color(0xFF161A17),
  scrim: Color(0xCC000000),
  textPrimary: Color(0xFFF5F5F5),
  textSecondary: Color(0xFFB0B5B1),
  digitFilledBg: Color(0xFF2E7D45),
  digitFilledText: Color(0xFFFFFFFF),
  digitFocusedBg: Color(0xFF161A17),
  digitFocusedOutline: Color(0xFFF5F5F5),
  digitEmptyBg: Color(0xFF262B27),
  digitEmptyOutline: Color(0xFF353B36),
  digitPlaceholder: Color(0xFF6F7570),
  buttonBg: Color(0xFF2E7D45),
  buttonText: Color(0xFFFFFFFF),
  buttonDisabledBg: Color(0xFF2A302B),
  link: Color(0xFF7BC795),
  success: Color(0xFF7BC795),
  error: Color(0xFFEF9A9A),
);

class _CountingService implements OtpService {
  final _mock = const MockOtpService(latency: Duration(milliseconds: 600));
  int verifyCalls = 0;
  int resendCalls = 0;

  @override
  Future<bool> verify(String code) {
    verifyCalls++;
    return _mock.verify(code);
  }

  @override
  Future<void> resendCode() {
    resendCalls++;
    return _mock.resendCode();
  }
}

Widget _wrap(
  OtpColors colors, {
  OtpService service = const MockOtpService(latency: Duration.zero),
}) {
  return MaterialApp(
    home: Scaffold(
      body: OtpScreen(colors: colors, service: service),
    ),
  );
}

Future<void> _pumpScreen(WidgetTester tester, [OtpService? service]) async {
  await tester.pumpWidget(
    service == null
        ? _wrap(_lightColors)
        : _wrap(_lightColors, service: service),
  );
  await tester.pump();
}

Key _digit(int n) => Key('otp_digit_$n');

TextField _field(WidgetTester tester, int n) =>
    tester.widget<TextField>(find.byKey(_digit(n)));

bool _noBoxFocused(WidgetTester tester) => [
  for (var n = 1; n <= 6; n++) _field(tester, n).focusNode!.hasFocus,
].every((f) => !f);

String _code(WidgetTester tester) =>
    [for (var n = 1; n <= 6; n++) _field(tester, n).controller!.text].join();

FilledButton _submitButton(WidgetTester tester) =>
    tester.widget<FilledButton>(find.byKey(const Key('otp_submit')));

BoxDecoration _decorationOf(WidgetTester tester, int n) {
  final box = tester.widget<Container>(
    find
        .ancestor(of: find.byKey(_digit(n)), matching: find.byType(Container))
        .first,
  );
  return box.decoration! as BoxDecoration;
}

Future<void> _submit(WidgetTester tester) async {
  await tester.tap(find.byKey(const Key('otp_submit')));
  await tester.pump(const Duration(milliseconds: 1));
  await tester.pump();
}

Future<void> _finishCountdown(WidgetTester tester) =>
    tester.pump(const Duration(seconds: 31));

void main() {
  testWidgets('autofocuses the first digit box', (tester) async {
    await _pumpScreen(tester);

    expect(_field(tester, 1).focusNode!.hasFocus, isTrue);
    expect(_field(tester, 1).keyboardType, TextInputType.number);
    await _finishCountdown(tester);
  });

  testWidgets('rejects non-digit input', (tester) async {
    await _pumpScreen(tester);

    await tester.enterText(find.byKey(_digit(1)), 'a');
    await tester.pump();

    expect(_field(tester, 1).controller!.text, isEmpty);
    await _finishCountdown(tester);
  });

  testWidgets('typing a digit advances focus to the next box', (tester) async {
    await _pumpScreen(tester);

    await tester.enterText(find.byKey(_digit(1)), '4');
    await tester.pump();

    expect(_field(tester, 2).focusNode!.hasFocus, isTrue);
    await _finishCountdown(tester);
  });

  testWidgets('backspace on a filled box clears only that digit', (
    tester,
  ) async {
    await _pumpScreen(tester);
    await tester.enterText(find.byKey(_digit(1)), '123456');
    await tester.pump();

    _field(tester, 3).focusNode!.requestFocus();
    await tester.pump();
    await tester.sendKeyEvent(LogicalKeyboardKey.backspace);
    await tester.pump();

    expect(_code(tester), '12456');
    expect(_field(tester, 3).focusNode!.hasFocus, isTrue);
    await _finishCountdown(tester);
  });

  testWidgets('backspace on an empty box clears the previous and moves back', (
    tester,
  ) async {
    await _pumpScreen(tester);

    await tester.enterText(find.byKey(_digit(1)), '4');
    await tester.pump();
    await tester.sendKeyEvent(LogicalKeyboardKey.backspace);
    await tester.pump();

    expect(_field(tester, 1).controller!.text, isEmpty);
    expect(_field(tester, 1).focusNode!.hasFocus, isTrue);
    await _finishCountdown(tester);
  });

  testWidgets('backspace on the first empty box keeps focus there', (
    tester,
  ) async {
    await _pumpScreen(tester);

    await tester.sendKeyEvent(LogicalKeyboardKey.backspace);
    await tester.pump();

    expect(_field(tester, 1).focusNode!.hasFocus, isTrue);
    await _finishCountdown(tester);
  });

  testWidgets(
    'typing into a filled box replaces its digit without touching others',
    (tester) async {
      await _pumpScreen(tester);
      await tester.enterText(find.byKey(_digit(1)), '123456');
      await tester.pump();

      await tester.tap(find.byKey(_digit(2)));
      await tester.pump();
      tester.testTextInput.updateEditingValue(
        const TextEditingValue(
          text: '27',
          selection: TextSelection.collapsed(offset: 2),
        ),
      );
      await tester.pump();

      expect(_code(tester), '173456');
      expect(_noBoxFocused(tester), isTrue);
      expect(_decorationOf(tester, 2).color, _lightColors.digitFilledBg);
      await _finishCountdown(tester);
    },
  );

  testWidgets('pasting a code fills all six boxes', (tester) async {
    await _pumpScreen(tester);

    await tester.enterText(find.byKey(_digit(1)), 'Your code is 482913');
    await tester.pump();

    expect(_code(tester), '482913');
    expect(_noBoxFocused(tester), isTrue);
    await _finishCountdown(tester);
  });

  testWidgets(
    'pasting fewer than six digits fills those and focuses the next box',
    (tester) async {
      await _pumpScreen(tester);

      await tester.enterText(find.byKey(_digit(1)), 'Code: 48');
      await tester.pump();

      expect(_code(tester), '48');
      expect(_field(tester, 3).focusNode!.hasFocus, isTrue);
      expect(_submitButton(tester).onPressed, isNull);
      await _finishCountdown(tester);
    },
  );

  testWidgets('Submit stays disabled until all six digits are filled', (
    tester,
  ) async {
    await _pumpScreen(tester);
    expect(_submitButton(tester).onPressed, isNull);

    await tester.enterText(find.byKey(_digit(1)), '12345');
    await tester.pump();
    expect(_submitButton(tester).onPressed, isNull);

    await tester.enterText(find.byKey(_digit(6)), '6');
    await tester.pump();
    expect(_submitButton(tester).onPressed, isNotNull);
    await _finishCountdown(tester);
  });

  testWidgets('the correct code shows the success message', (tester) async {
    await _pumpScreen(tester);

    await tester.enterText(find.byKey(_digit(1)), '123456');
    await tester.pump();
    await _submit(tester);

    expect(find.text("You're in."), findsOneWidget);
    await _finishCountdown(tester);
  });

  testWidgets(
    'a wrong code shows the error, clears boxes and refocuses the first',
    (tester) async {
      await _pumpScreen(tester);

      await tester.enterText(find.byKey(_digit(1)), '000000');
      await tester.pump();
      await _submit(tester);

      expect(find.text("That code didn't work. Try again."), findsOneWidget);
      expect(_code(tester), isEmpty);
      expect(_field(tester, 1).focusNode!.hasFocus, isTrue);
      expect(_submitButton(tester).onPressed, isNull);
      await _finishCountdown(tester);
    },
  );

  testWidgets('Submit is blocked while a verification is in progress', (
    tester,
  ) async {
    final service = _CountingService();
    await _pumpScreen(tester, service);
    await tester.enterText(find.byKey(_digit(1)), '123456');
    await tester.pump();

    await tester.tap(find.byKey(const Key('otp_submit')));
    await tester.pump();
    expect(_submitButton(tester).onPressed, isNull);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    await tester.tap(find.byKey(const Key('otp_submit')));

    await tester.pump(const Duration(milliseconds: 600));
    expect(service.verifyCalls, 1);
    expect(find.text("You're in."), findsOneWidget);
    await _finishCountdown(tester);
  });

  testWidgets(
    'Resend is disabled for 30s, then resends once and restarts the countdown',
    (tester) async {
      final service = _CountingService();
      await _pumpScreen(tester, service);

      expect(find.text('Resend in 0:30'), findsOneWidget);
      expect(
        tester
            .widget<TextButton>(find.byKey(const Key('otp_resend')))
            .onPressed,
        isNull,
      );

      await tester.pump(const Duration(seconds: 30));
      expect(find.text('Resend'), findsOneWidget);
      expect(find.text("Didn't receive the code? "), findsOneWidget);

      await tester.tap(find.byKey(const Key('otp_resend')));
      await tester.pump();
      expect(service.resendCalls, 1);
      expect(find.text('Resend in 0:30'), findsOneWidget);
      expect(
        tester
            .widget<TextButton>(find.byKey(const Key('otp_resend')))
            .onPressed,
        isNull,
      );
      await _finishCountdown(tester);
    },
  );

  testWidgets('each digit box is announced as "Digit N of 6" with its value', (
    tester,
  ) async {
    final handle = tester.ensureSemantics();
    await _pumpScreen(tester);
    await tester.enterText(find.byKey(_digit(1)), '7');
    await tester.pump();

    for (var n = 1; n <= 6; n++) {
      expect(tester.getSemantics(find.byKey(_digit(n))).label, 'Digit $n of 6');
    }
    expect(tester.getSemantics(find.byKey(_digit(1))).value, '7');
    handle.dispose();
    await _finishCountdown(tester);
  });

  testWidgets(
    'boxes show filled, focused and empty states with a placeholder',
    (tester) async {
      await _pumpScreen(tester);

      await tester.enterText(find.byKey(_digit(1)), '4');
      await tester.pump();

      final filled = _decorationOf(tester, 1);
      expect(filled.color, _lightColors.digitFilledBg);
      expect(filled.border, isNull);

      final focused = _decorationOf(tester, 2);
      expect(focused.color, _lightColors.digitFocusedBg);
      expect(
        (focused.border! as Border).top.color,
        _lightColors.digitFocusedOutline,
      );

      expect(_decorationOf(tester, 3).color, _lightColors.digitEmptyBg);
      expect(_field(tester, 3).decoration!.hintText, isNot('0'));
      await _finishCountdown(tester);
    },
  );

  testWidgets(
    'a complete code shows every box as filled: dark green with a white digit',
    (tester) async {
      await _pumpScreen(tester);

      for (var n = 1; n <= 6; n++) {
        await tester.enterText(find.byKey(_digit(n)), '$n');
        await tester.pump();
      }

      expect(_noBoxFocused(tester), isTrue);
      for (var n = 1; n <= 6; n++) {
        final box = _decorationOf(tester, n);
        expect(box.color, _lightColors.digitFilledBg);
        expect(box.border, isNull);
        expect(_field(tester, n).style!.color, _lightColors.digitFilledText);
      }
      await _finishCountdown(tester);
    },
  );

  testWidgets('dark mode uses the dark palette colours', (tester) async {
    await tester.pumpWidget(_wrap(_darkColors));
    await tester.pump();

    expect(_decorationOf(tester, 1).color, _darkColors.digitFocusedBg);
    expect(_decorationOf(tester, 2).color, _darkColors.digitEmptyBg);
    await _finishCountdown(tester);
  });

  group('layout', () {
    const palette = OtpPalette(light: _lightColors, dark: _darkColors);

    Future<void> pumpApp(
      WidgetTester tester,
      Size size, {
      double keyboard = 0,
    }) async {
      tester.view.devicePixelRatio = 1;
      tester.view.physicalSize = size;
      tester.view.viewInsets = FakeViewPadding(bottom: keyboard);
      addTearDown(tester.view.reset);
      await tester.pumpWidget(const HakkaApp(palette: palette));
      await tester.pump();
    }

    for (final (name, size) in [
      ('small phone', const Size(320, 568)),
      ('large phone', const Size(430, 932)),
      ('landscape', const Size(844, 390)),
    ]) {
      testWidgets('fits a $name without overflow', (tester) async {
        await pumpApp(tester, size);

        expect(tester.takeException(), isNull);
        final row = [
          for (var n = 1; n <= 6; n++)
            tester.getCenter(find.byKey(_digit(n))).dy,
        ];
        expect(row.toSet(), hasLength(1));
        await _finishCountdown(tester);
      });
    }

    testWidgets('Submit stays reachable while the keyboard is open', (
      tester,
    ) async {
      await pumpApp(tester, const Size(390, 844), keyboard: 320);

      await tester.ensureVisible(find.byKey(const Key('otp_submit')));
      await tester.pump();

      expect(tester.takeException(), isNull);
      expect(
        tester.getRect(find.byKey(const Key('otp_submit'))).bottom,
        lessThanOrEqualTo(844 - 320),
      );
      await _finishCountdown(tester);
    });
  });
}
