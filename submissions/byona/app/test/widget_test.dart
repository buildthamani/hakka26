import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/main.dart';
import 'package:hakka/otp/otp_colors.dart';

// The blinking caret and the countdown never settle, so these tests advance
// time with pump() and must not use pumpAndSettle().

const otpLength = 6;

Finder box(int position) => find.byKey(Key('otp_digit_$position'));
final submit = find.byKey(const Key('otp_submit'));
final resend = find.byKey(const Key('otp_resend'));
final status = find.byKey(const Key('otp_status'));

TextField fieldAt(WidgetTester tester, int position) =>
    tester.widget<TextField>(box(position));

bool hasFocus(WidgetTester tester, int position) =>
    fieldAt(tester, position).focusNode!.hasFocus;

/// The digit a box shows, without the invisible character kept in the field.
String digitAt(WidgetTester tester, int position) =>
    fieldAt(tester, position).controller!.text.replaceAll('​', '');

String allDigits(WidgetTester tester) => [
  for (var position = 1; position <= otpLength; position++)
    digitAt(tester, position),
].join();

BoxDecoration decorationOf(WidgetTester tester, int position) {
  final container = find.ancestor(
    of: box(position),
    matching: find.byType(AnimatedContainer),
  );
  return tester.widget<AnimatedContainer>(container).decoration!
      as BoxDecoration;
}

String statusText(WidgetTester tester) => tester.widget<Text>(status).data!;

bool isEnabled(WidgetTester tester, Finder button) =>
    tester.widget<ButtonStyleButton>(button).enabled;

Future<void> pumpOtp(WidgetTester tester) async {
  await tester.pumpWidget(const HakkaApp());
  await tester.pump();
}

/// Sends the edit a soft keyboard would send to the focused box.
Future<void> imeEdit(
  WidgetTester tester,
  String Function(String current) edit,
) async {
  final current = tester.testTextInput.editingState!['text'] as String;
  final next = edit(current);
  tester.testTextInput.updateEditingValue(
    TextEditingValue(
      text: next,
      selection: TextSelection.collapsed(offset: next.length),
    ),
  );
  await tester.pump();
}

Future<void> imeType(WidgetTester tester, String digits) async {
  for (final digit in digits.split('')) {
    await imeEdit(tester, (current) => '$current$digit');
  }
}

Future<void> imeBackspace(WidgetTester tester) =>
    imeEdit(tester, (current) => current.substring(0, current.length - 1));

void mockClipboard(WidgetTester tester, String text) {
  tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
    SystemChannels.platform,
    (call) async => switch (call.method) {
      'Clipboard.getData' => {'text': text},
      'Clipboard.hasStrings' => {'value': true},
      _ => null,
    },
  );
  addTearDown(
    () => tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
      SystemChannels.platform,
      null,
    ),
  );
}

void main() {
  testWidgets('shows the exact copy', (tester) async {
    await pumpOtp(tester);

    expect(find.text('Enter your code'), findsOneWidget);
    expect(
      find.text('A 6-digit code has been sent to your phone.'),
      findsOneWidget,
    );
    expect(find.text('Resend in 0:30'), findsOneWidget);
    expect(find.text('Submit'), findsOneWidget);
    expect(status, findsOneWidget);
  });

  group('AC1: six circular digit boxes', () {
    testWidgets('renders six circles with number pad, no maxLength', (
      tester,
    ) async {
      await pumpOtp(tester);

      for (var position = 1; position <= otpLength; position++) {
        final field = fieldAt(tester, position);
        expect(field.keyboardType, TextInputType.number);
        expect(field.maxLength, isNull);
        expect(decorationOf(tester, position).shape, BoxShape.circle);
        final size = tester.getSize(
          find.ancestor(
            of: box(position),
            matching: find.byType(AnimatedContainer),
          ),
        );
        expect(size.width, size.height);
      }
      expect(find.byType(TextField), findsNWidgets(otpLength));
    });

    testWidgets('focuses the first box and opens the keyboard', (tester) async {
      await pumpOtp(tester);

      expect(hasFocus(tester, 1), isTrue);
      expect(tester.testTextInput.isVisible, isTrue);
      expect(
        tester.testTextInput.setClientArgs!['inputType']['name'],
        'TextInputType.number',
      );
    });

    testWidgets('rejects non-digits', (tester) async {
      await pumpOtp(tester);

      await imeType(tester, 'a');

      expect(allDigits(tester), isEmpty);
      expect(hasFocus(tester, 1), isTrue);
    });
  });

  group('AC2: box states', () {
    testWidgets('filled, focused and empty use their palette colours', (
      tester,
    ) async {
      await pumpOtp(tester);
      await imeType(tester, '2');

      const colors = OtpColors.light;
      final filled = decorationOf(tester, 1);
      final focused = decorationOf(tester, 2);
      final empty = decorationOf(tester, 3);

      expect(filled.color, colors.digitFilledBg);
      expect(fieldAt(tester, 1).style!.color, colors.digitFilledText);
      expect(focused.color, colors.digitFocusedBg);
      expect((focused.border! as Border).top.color, colors.digitFocusedOutline);
      expect(empty.color, colors.digitEmptyBg);
    });

    testWidgets('a focused box that holds a digit keeps the focused style', (
      tester,
    ) async {
      await pumpOtp(tester);
      await imeType(tester, '21');

      await tester.tap(box(1));
      await tester.pump();

      expect(hasFocus(tester, 1), isTrue);
      expect(decorationOf(tester, 1).color, OtpColors.light.digitFocusedBg);
      expect(fieldAt(tester, 1).style!.color, OtpColors.light.textPrimary);
    });

    testWidgets('empty boxes show a dash, never a real zero', (tester) async {
      await pumpOtp(tester);

      expect(find.text('–'), findsNWidgets(otpLength - 1));
      expect(find.text('0'), findsNothing);
    });
  });

  testWidgets('AC3: typing a digit moves focus to the next box', (
    tester,
  ) async {
    await pumpOtp(tester);

    for (var position = 1; position < otpLength; position++) {
      await imeType(tester, '$position');
      expect(digitAt(tester, position), '$position');
      expect(hasFocus(tester, position + 1), isTrue);
    }
  });

  testWidgets('tester.enterText works on every box by test ID', (tester) async {
    await pumpOtp(tester);

    for (var position = 1; position <= otpLength; position++) {
      await tester.enterText(box(position), '$position');
      await tester.pump();
    }

    expect(allDigits(tester), '123456');
  });

  group('AC4: backspace', () {
    testWidgets('from a soft keyboard on an empty box clears the previous', (
      tester,
    ) async {
      await pumpOtp(tester);
      await imeType(tester, '12');
      expect(hasFocus(tester, 3), isTrue);

      await imeBackspace(tester);

      expect(allDigits(tester), '1');
      expect(hasFocus(tester, 2), isTrue);
    });

    testWidgets('from a hardware key on an empty box clears the previous', (
      tester,
    ) async {
      await pumpOtp(tester);
      await imeType(tester, '12');

      await tester.sendKeyEvent(LogicalKeyboardKey.backspace);
      await tester.pump();

      expect(allDigits(tester), '1');
      expect(hasFocus(tester, 2), isTrue);
    });

    testWidgets('repeated presses walk back through every box', (tester) async {
      await pumpOtp(tester);
      await imeType(tester, '123');

      for (final expected in ['12', '1', '']) {
        await imeBackspace(tester);
        expect(allDigits(tester), expected);
      }
      expect(hasFocus(tester, 1), isTrue);
    });

    testWidgets('still works after a tap puts the caret before the sentinel', (
      tester,
    ) async {
      await pumpOtp(tester);
      await imeType(tester, '12');

      await tester.tapAt(tester.getTopLeft(box(3)) + const Offset(2, 24));
      await tester.pump(const Duration(milliseconds: 400));
      await tester.sendKeyEvent(LogicalKeyboardKey.backspace);
      await tester.pump();

      expect(allDigits(tester), '1');
      expect(hasFocus(tester, 2), isTrue);
    });

    testWidgets('on a filled box clears it and stays', (tester) async {
      await pumpOtp(tester);
      await imeType(tester, '123456');
      expect(hasFocus(tester, otpLength), isTrue);

      await imeBackspace(tester);

      expect(allDigits(tester), '12345');
      expect(hasFocus(tester, otpLength), isTrue);
    });
  });

  group('AC5: pasting', () {
    testWidgets('text from the keyboard fills all six boxes', (tester) async {
      await pumpOtp(tester);

      await imeEdit(tester, (current) => '${current}Your code is 482913');

      expect(allDigits(tester), '482913');
      expect(hasFocus(tester, otpLength), isTrue);
      expect(isEnabled(tester, submit), isTrue);
    });

    testWidgets('over a box that already holds a digit is not misread', (
      tester,
    ) async {
      await pumpOtp(tester);
      await imeType(tester, '4');
      await tester.tap(box(1));
      await tester.pump();

      await imeEdit(tester, (current) => '${current}Your code is 482913');

      expect(allDigits(tester), '482913');
    });

    testWidgets('from the long-press menu fills all six boxes', (tester) async {
      mockClipboard(tester, 'Your code is 482913');
      await pumpOtp(tester);

      await tester.longPress(box(1));
      await tester.pump(const Duration(milliseconds: 300));
      await tester.tap(find.text('Paste'));
      await tester.pump();
      await tester.pump();

      expect(allDigits(tester), '482913');
    });
  });

  testWidgets('AC6: Submit stays disabled until all six digits are filled', (
    tester,
  ) async {
    await pumpOtp(tester);
    expect(isEnabled(tester, submit), isFalse);

    await imeType(tester, '12345');
    expect(isEnabled(tester, submit), isFalse);

    await imeType(tester, '6');
    expect(isEnabled(tester, submit), isTrue);

    await imeBackspace(tester);
    expect(isEnabled(tester, submit), isFalse);
  });

  testWidgets('AC7: 123456 shows the success message', (tester) async {
    await pumpOtp(tester);
    await imeType(tester, '123456');

    await tester.tap(submit);
    await tester.pump();

    expect(statusText(tester), "You're in.");
    expect(tester.widget<Text>(status).style!.color, OtpColors.light.success);
  });

  testWidgets(
    'AC8: another code shows the error, clears the boxes, refocuses the first',
    (tester) async {
      await pumpOtp(tester);
      await imeType(tester, '111111');

      await tester.tap(submit);
      await tester.pump();

      expect(statusText(tester), "That code didn't work. Try again.");
      expect(tester.widget<Text>(status).style!.color, OtpColors.light.error);
      expect(allDigits(tester), isEmpty);
      expect(hasFocus(tester, 1), isTrue);
      expect(isEnabled(tester, submit), isFalse);

      await imeType(tester, '1');
      expect(statusText(tester), isEmpty);
      expect(digitAt(tester, 1), '1');
    },
  );

  group('AC9: resend', () {
    testWidgets('counts down, unlocks, and restarts when tapped', (
      tester,
    ) async {
      await pumpOtp(tester);
      expect(find.text('Resend in 0:30'), findsOneWidget);
      expect(isEnabled(tester, resend), isFalse);

      await tester.pump(const Duration(seconds: 21));
      expect(find.text('Resend in 0:09'), findsOneWidget);
      expect(isEnabled(tester, resend), isFalse);

      await tester.pump(const Duration(seconds: 9));
      expect(find.text("Didn't receive the code? "), findsOneWidget);
      expect(find.text('Resend'), findsOneWidget);
      expect(isEnabled(tester, resend), isTrue);

      await tester.tap(resend);
      await tester.pump();
      expect(find.text('Resend in 0:30'), findsOneWidget);
      expect(find.text("Didn't receive the code? "), findsNothing);
      expect(isEnabled(tester, resend), isFalse);
    });

    testWidgets('a tick leaves focus where the user moved it', (tester) async {
      await pumpOtp(tester);
      await imeType(tester, '12');
      await tester.tap(box(1));
      await tester.pump();

      await tester.pump(const Duration(seconds: 3));

      expect(hasFocus(tester, 1), isTrue);
    });
  });

  group('AC10: accessibility and dark mode', () {
    testWidgets('each box is announced as Digit N of 6', (tester) async {
      final semantics = tester.ensureSemantics();
      await pumpOtp(tester);

      for (var position = 1; position <= otpLength; position++) {
        expect(
          tester.getSemantics(box(position)),
          isSemantics(label: 'Digit $position of 6', isTextField: true),
        );
      }
      semantics.dispose();
    });

    testWidgets('the placeholder is hidden from screen readers', (
      tester,
    ) async {
      final semantics = tester.ensureSemantics();
      await pumpOtp(tester);

      expect(find.bySemanticsLabel('–'), findsNothing);
      semantics.dispose();
    });

    testWidgets('dark mode uses the dark palette and white logo', (
      tester,
    ) async {
      tester.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
      addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);

      await pumpOtp(tester);
      await imeType(tester, '2');

      const colors = OtpColors.dark;
      expect(decorationOf(tester, 1).color, colors.digitFilledBg);
      expect(decorationOf(tester, 2).color, colors.digitFocusedBg);
      expect(decorationOf(tester, 3).color, colors.digitEmptyBg);
      expect(
        find.image(const AssetImage('assets/thamani-logo-white.png')),
        findsOneWidget,
      );
      expect(
        find.byWidgetPredicate(
          (widget) => widget is Material && widget.color == colors.sheet,
        ),
        findsOneWidget,
      );
    });
  });

  group('layout', () {
    void setScreen(
      WidgetTester tester, {
      required Size size,
      double keyboard = 0,
    }) {
      tester.view.devicePixelRatio = 1;
      tester.view.physicalSize = size;
      tester.view.viewInsets = FakeViewPadding(bottom: keyboard);
      addTearDown(tester.view.reset);
    }

    const screens = [
      ('small phone', Size(320, 568), 260.0),
      ('common phone', Size(360, 640), 280.0),
      ('tall phone', Size(412, 915), 340.0),
      ('landscape phone', Size(800, 360), 200.0),
    ];

    for (final (name, size, keyboard) in screens) {
      testWidgets('$name: the keyboard causes no overflow', (tester) async {
        setScreen(tester, size: size, keyboard: keyboard);

        await pumpOtp(tester);

        expect(tester.takeException(), isNull);
        final keyboardTop = size.height - keyboard;
        expect(tester.getBottomLeft(submit).dy, lessThan(keyboardTop));
        expect(submit.hitTestable(), findsOneWidget);
      });
    }

    testWidgets('large text on a small phone causes no overflow', (
      tester,
    ) async {
      setScreen(tester, size: const Size(320, 568), keyboard: 260);
      tester.platformDispatcher.textScaleFactorTestValue = 2;
      addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);

      await pumpOtp(tester);
      await tester.pump(const Duration(seconds: 30));

      expect(tester.takeException(), isNull);
    });

    testWidgets('the keypad action key keeps the box focused', (tester) async {
      await pumpOtp(tester);

      await tester.testTextInput.receiveAction(TextInputAction.done);
      await tester.pump();

      expect(hasFocus(tester, 1), isTrue);
    });
  });
}
