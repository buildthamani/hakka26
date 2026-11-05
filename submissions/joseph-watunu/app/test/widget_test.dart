import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/main.dart';
import 'package:hakka/otp_colors.dart';
import 'package:hakka/otp_input.dart';
import 'package:hakka/otp_screen.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  late OtpColors colors;
  late DateTime now;
  final input = find.byKey(const ValueKey('otp_input'));
  final submit = find.byKey(const ValueKey('otp_submit'));
  final resend = find.byKey(const ValueKey('otp_resend'));
  Finder digit(int i) => find.byKey(ValueKey('otp_digit_$i'));
  TextField field(WidgetTester t) => t.widget<TextField>(input);
  bool focused(WidgetTester t, int i) =>
      t.widget<Semantics>(digit(i)).properties.focused!;
  BoxDecoration circle(WidgetTester t, int i) =>
      t.widget<Container>(find.byKey(ValueKey('otp_circle_$i'))).decoration!
          as BoxDecoration;
  bool canSubmit(WidgetTester t) =>
      t.widget<FilledButton>(submit).onPressed != null;
  bool canResend(WidgetTester t) =>
      t.widget<TextButton>(resend).onPressed != null;

  setUpAll(() async => colors = await OtpColors.load());
  setUp(() {
    now = DateTime(2026, 11, 5, 12);
    String? clipboard;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(SystemChannels.platform, (call) async {
          if (call.method == 'Clipboard.setData') {
            clipboard = (call.arguments as Map)['text'] as String?;
          }
          if (call.method == 'Clipboard.getData') return {'text': clipboard};
          if (call.method == 'Clipboard.hasStrings')
            return {'value': clipboard?.isNotEmpty ?? false};
          return null;
        });
  });

  Future<void> pumpScreen(
    WidgetTester t, {
    Brightness brightness = Brightness.light,
    Size size = const Size(393, 824),
    double textScale = 1,
    double keyboard = 0,
  }) async {
    t.view.devicePixelRatio = 1;
    t.view.physicalSize = size;
    addTearDown(t.view.resetPhysicalSize);
    addTearDown(t.view.resetDevicePixelRatio);
    await t.pumpWidget(
      MaterialApp(
        theme: ThemeData(brightness: brightness),
        builder: (context, child) => MediaQuery(
          data: MediaQuery.of(context).copyWith(
            textScaler: TextScaler.linear(textScale),
            viewInsets: EdgeInsets.only(bottom: keyboard),
          ),
          child: child!,
        ),
        home: OtpScreen(colors: colors, now: () => now),
      ),
    );
    await t.pumpAndSettle();
  }

  Future<void> enter(WidgetTester t, String text) async {
    t.testTextInput.enterText(text);
    await t.pump();
  }

  testWidgets(
    'startup keyboard retry stops after visibility and respects dismissal',
    (t) async {
      t.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
      await pumpScreen(t);
      t.testTextInput.log.clear();
      await t.pump(const Duration(milliseconds: 150));
      expect(
        t.testTextInput.log.where((call) => call.method == 'TextInput.show'),
        isNotEmpty,
      );
      t.view.viewInsets = const FakeViewPadding(bottom: 280);
      await t.pump();
      t.view.resetViewInsets();
      await t.pump();
      t.testTextInput.log.clear();
      await t.pump(const Duration(seconds: 2));
      expect(
        t.testTextInput.log.where((call) => call.method == 'TextInput.show'),
        isEmpty,
      );
    },
  );

  testWidgets(
    'exact copy, placeholders, disabled submit and automatic numeric keyboard',
    (t) async {
      await pumpScreen(t);
      for (final copy in [
        'Enter your code',
        'A 6-digit code has been sent to your phone.',
        "Didn't receive the code? ",
        'Submit',
        'Resend in 0:30',
      ]) {
        expect(find.text(copy), findsOneWidget);
      }
      expect(find.text('–'), findsNWidgets(6));
      expect(find.text('0'), findsNothing);
      expect(t.testTextInput.isVisible, isTrue);
      expect(field(t).keyboardType, TextInputType.number);
      expect(field(t).autofillHints, contains(AutofillHints.oneTimeCode));
      expect(canSubmit(t), isFalse);
      expect(canResend(t), isFalse);
      expect(field(t).focusNode!.hasFocus, isTrue);
      expect(circle(t, 1).color, colors.light['digit_focused_bg']);
      expect(circle(t, 2).color, colors.light['digit_empty_bg']);
    },
  );

  testWidgets('typing advances focus and permits only six ASCII digits', (
    t,
  ) async {
    await pumpScreen(t);
    for (var count = 1; count <= 5; count++) {
      await enter(t, '12345'.substring(0, count));
      expect(focused(t, count + 1), isTrue);
      expect(circle(t, count).color, colors.light['digit_filled_bg']);
      expect(circle(t, count).shape, BoxShape.circle);
      expect(canSubmit(t), isFalse);
    }
    await enter(t, '123456');
    expect(canSubmit(t), isTrue);
    await enter(t, '1234567abc');
    expect(field(t).controller!.text, '123456');
    await enter(t, 'a1-2 3🦋٤');
    expect(field(t).controller!.text, '123');
  });

  testWidgets(
    'backspace on empty cell clears previous digit and moves focus back',
    (t) async {
      await pumpScreen(t);
      await enter(t, '21');
      expect(focused(t, 3), isTrue);
      await t.sendKeyEvent(LogicalKeyboardKey.backspace);
      await t.pump();
      expect(field(t).controller!.text, '2');
      expect(focused(t, 2), isTrue);
      await t.sendKeyEvent(LogicalKeyboardKey.backspace);
      await t.sendKeyEvent(LogicalKeyboardKey.backspace);
      await t.pump();
      expect(field(t).controller!.text, isEmpty);
      expect(focused(t, 1), isTrue);
    },
  );

  testWidgets(
    'soft deletion and editing a selected digit retain native caret behavior',
    (t) async {
      await pumpScreen(t);
      await enter(t, '215');
      t.testTextInput.updateEditingValue(
        const TextEditingValue(
          text: '21',
          selection: TextSelection.collapsed(offset: 2),
        ),
      );
      await t.pump();
      expect(focused(t, 3), isTrue);
      await t.tap(digit(1));
      await t.pump();
      expect(
        field(t).controller!.selection,
        const TextSelection(baseOffset: 0, extentOffset: 1),
      );
      t.testTextInput.updateEditingValue(
        const TextEditingValue(
          text: '91',
          selection: TextSelection.collapsed(offset: 1),
        ),
      );
      await t.pump();
      expect(field(t).controller!.text, '91');
      expect(focused(t, 2), isTrue);
    },
  );

  testWidgets(
    'native SMS paste strips words and replaces a partially filled code',
    (t) async {
      await pumpScreen(t);
      await enter(t, '21');
      await enter(t, '21Your code is 482913');
      expect(field(t).controller!.text, '482913');
      for (var i = 1; i <= 6; i++) {
        expect(t.widget<Semantics>(digit(i)).properties.value, '482913'[i - 1]);
      }
      expect(canSubmit(t), isTrue);
    },
  );

  testWidgets('long press Paste reads the platform clipboard', (t) async {
    await pumpScreen(t);
    await Clipboard.setData(const ClipboardData(text: 'Your code is 482913'));
    await t.longPress(digit(2));
    await t.pumpAndSettle();
    await t.tap(find.text('Paste'));
    await t.pumpAndSettle();
    expect(field(t).controller!.text, '482913');
  });

  testWidgets('platform autofill fills the code and keyboard Done validates', (
    t,
  ) async {
    await pumpScreen(t);
    final editable = t.state<EditableTextState>(find.byType(EditableText));
    final message = const JSONMethodCodec().encodeMethodCall(
      MethodCall('TextInputClient.updateEditingStateWithTag', [
        0,
        {
          editable.autofillId: const TextEditingValue(
            text: '123456',
            selection: TextSelection.collapsed(offset: 6),
          ).toJSON(),
        },
      ]),
    );
    await t.binding.defaultBinaryMessenger.handlePlatformMessage(
      'flutter/textinput',
      message,
      (_) {},
    );
    await t.pump();
    expect(field(t).controller!.text, '123456');
    await t.testTextInput.receiveAction(TextInputAction.done);
    await t.pump();
    expect(find.text("You're in."), findsOneWidget);
    expect(field(t).focusNode!.hasFocus, isFalse);
  });

  testWidgets(
    'Submit validates success and error clears every digit and restores focus',
    (t) async {
      await pumpScreen(t);
      await enter(t, '482913');
      await t.tap(submit);
      await t.pump();
      expect(find.text("That code didn't work. Try again."), findsOneWidget);
      expect(field(t).controller!.text, isEmpty);
      expect(field(t).focusNode!.hasFocus, isTrue);
      expect(focused(t, 1), isTrue);
      expect(canSubmit(t), isFalse);
      await enter(t, '123456');
      expect(find.text("That code didn't work. Try again."), findsNothing);
      await t.tap(submit);
      await t.pump();
      expect(find.text("You're in."), findsOneWidget);
    },
  );

  testWidgets(
    'resend unlocks at 30 seconds, not before, and restarts when tapped',
    (t) async {
      await pumpScreen(t);
      await t.tap(resend);
      now = now.add(const Duration(seconds: 29));
      await t.pump(const Duration(seconds: 29));
      expect(find.text('Resend in 0:01'), findsOneWidget);
      expect(canResend(t), isFalse);
      now = now.add(const Duration(seconds: 1));
      await t.pump(const Duration(seconds: 1));
      expect(find.text('Resend'), findsOneWidget);
      await t.tap(resend);
      await t.pump();
      expect(find.text('Resend in 0:30'), findsOneWidget);
      expect(canResend(t), isFalse);
    },
  );

  testWidgets('returning from background expires the deadline immediately', (
    t,
  ) async {
    await pumpScreen(t);
    t.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    t.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    t.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    now = now.add(const Duration(seconds: 45));
    t.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    t.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    t.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await t.pump();
    expect(find.text('Resend'), findsOneWidget);
    expect(canResend(t), isTrue);
  });

  testWidgets('screen readers get editable digit labels, IDs and live status', (
    t,
  ) async {
    final semantics = t.ensureSemantics();
    try {
      await pumpScreen(t);
      for (var i = 1; i <= 6; i++) {
        final data = t.getSemantics(digit(i)).getSemanticsData();
        expect(data.label, 'Digit $i of 6');
        expect(data.identifier, 'otp_digit_$i');
        expect(data.hasAction(SemanticsAction.setText), isTrue);
      }
      t.semantics.setText(find.semantics.byLabel('Digit 1 of 6'), '1');
      await t.pump();
      expect(field(t).controller!.text, '1');
      expect(
        t
            .widget<Semantics>(find.byKey(const ValueKey('otp_status')))
            .properties
            .liveRegion,
        isTrue,
      );
      expect(
        t.getSemantics(submit).getSemanticsData().identifier,
        'otp_submit',
      );
      expect(
        t.getSemantics(resend).getSemanticsData().identifier,
        'otp_resend',
      );
    } finally {
      semantics.dispose();
    }
  });

  testWidgets('dark mode uses supplied sheet, digit, error and logo tokens', (
    t,
  ) async {
    await pumpScreen(t, brightness: Brightness.dark);
    await enter(t, '21');
    final sheet = t.widget<Container>(find.byKey(const ValueKey('otp_sheet')));
    expect((sheet.decoration! as BoxDecoration).color, colors.dark['sheet']);
    expect(circle(t, 1).color, colors.dark['digit_filled_bg']);
    expect(circle(t, 3).color, colors.dark['digit_focused_bg']);
    expect(circle(t, 4).color, colors.dark['digit_empty_bg']);
    expect(
      (t.widget<Image>(find.byType(Image)).image as AssetImage).assetName,
      'assets/thamani-logo-white.png',
    );
    await enter(t, '000000');
    await t.tap(submit);
    await t.pump();
    expect(
      t
          .widget<Text>(find.text("That code didn't work. Try again."))
          .style!
          .color,
      colors.dark['error'],
    );
  });

  for (final size in [const Size(320, 568), const Size(824, 393)]) {
    testWidgets('large text and keyboard remain scrollable at $size', (
      t,
    ) async {
      await pumpScreen(t, size: size, textScale: 2, keyboard: 180);
      expect(t.takeException(), isNull);
      await enter(t, '123456');
      await t.scrollUntilVisible(
        submit,
        120,
        scrollable: find.byType(Scrollable).first,
      );
      await t.pumpAndSettle();
      expect(submit.hitTestable(), findsOneWidget);
      await t.tap(submit);
      await t.pump();
      expect(find.text("You're in."), findsOneWidget);
      expect(t.takeException(), isNull);
    });
  }

  testWidgets('app follows system theme', (t) async {
    t.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
    addTearDown(t.platformDispatcher.clearPlatformBrightnessTestValue);
    await t.pumpWidget(HakkaApp(colors: colors));
    await t.pumpAndSettle();
    expect(
      Theme.of(t.element(find.byType(OtpScreen))).brightness,
      Brightness.dark,
    );
  });

  testWidgets('disposing an active countdown leaves no timer or focus errors', (
    t,
  ) async {
    await pumpScreen(t);
    await t.pumpWidget(const SizedBox.shrink());
    await t.pump(const Duration(seconds: 31));
    expect(t.takeException(), isNull);
  });

  test(
    'formatter preserves leading zeroes and full paste over a selection',
    () {
      final formatter = OtpInputFormatter();
      final result = formatter.formatEditUpdate(
        const TextEditingValue(
          text: '000',
          selection: TextSelection(baseOffset: 0, extentOffset: 3),
        ),
        const TextEditingValue(
          text: 'Your code is 482913',
          selection: TextSelection.collapsed(offset: 19),
        ),
      );
      expect(result.text, '482913');
      expect(result.selection.baseOffset, 6);
      expect(
        formatter
            .formatEditUpdate(
              TextEditingValue.empty,
              const TextEditingValue(
                text: '001234',
                selection: TextSelection.collapsed(offset: 6),
              ),
            )
            .text,
        '001234',
      );
    },
  );
}
