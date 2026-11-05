import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/main.dart';
import 'package:hakka/otp_controller.dart';
import 'package:hakka/otp_palette.dart';

Finder input(int n) => find.byKey(const ValueKey('otp_input'));
Finder digit(int n) => find.byKey(ValueKey('otp_digit_$n'));
Finder get submit => find.byKey(const ValueKey('otp_submit'));
Finder get resend => find.byKey(const ValueKey('otp_resend'));

Future<void> typeAt(WidgetTester tester, int n, String text) async {
  await tester.tap(digit(n));
  await tester.pump();
  final old = tester.widget<TextField>(input(n)).controller!.value;
  final start = old.selection.start;
  tester.testTextInput.updateEditingValue(
    TextEditingValue(
      text: old.text.replaceRange(start, old.selection.end, text),
      selection: TextSelection.collapsed(offset: start + text.length),
    ),
  );
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  late OtpColors colors;
  setUpAll(() async {
    colors = await OtpColors.load();
  });

  Future<void> launch(
    WidgetTester tester, {
    DateTime Function()? now,
    ThemeMode mode = ThemeMode.light,
  }) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(
      HakkaApp(colors: colors, now: now, themeMode: mode),
    );
    await tester.pump();
  }

  Future<void> finish(WidgetTester tester) async {
    await tester.pumpWidget(const SizedBox());
  }

  bool focused(WidgetTester tester, int n) =>
      tester.widget<Semantics>(digit(n)).properties.focused == true;
  String value(WidgetTester tester, int n) =>
      tester.widget<Semantics>(digit(n)).properties.value!;

  test(
    'controller preserves zero, replaces digits and handles bounded paste/deletion',
    () {
      final otp = OtpController();
      addTearDown(otp.dispose);
      otp.enter(0, '0');
      expect(otp.digitAt(0), '0');
      expect(otp.isComplete, false);
      otp.enter(4, '89');
      expect(otp.isComplete, false);
      otp.enter(3, 'Your code is 012345678');
      expect(otp.code, '012345');
      expect(otp.isComplete, true);
      otp.enter(2, '9');
      expect(otp.code, '019345');
      otp.backspace(2);
      expect(otp.code, '01345');
      otp.backspace(5);
      expect(otp.code, '0134');
      expect(otp.activeIndex, 4);
    },
  );

  testWidgets(
    'autofocus, numeric keyboard, empty placeholders and disabled Submit',
    (tester) async {
      await launch(tester);
      expect(focused(tester, 1), true);
      expect(tester.testTextInput.isVisible, true);
      expect(
        tester.widget<TextField>(input(1)).keyboardType,
        TextInputType.number,
      );
      expect(tester.widget<FilledButton>(submit).onPressed, isNull);
      expect(find.text('–'), findsNWidgets(6));
      expect(find.text('0'), findsNothing);
      expect(find.text('Enter your code'), findsOneWidget);
      expect(
        find.text('A 6-digit code has been sent to your phone.'),
        findsOneWidget,
      );
      await finish(tester);
    },
  );

  testWidgets(
    'digit entry advances focus; letters are rejected; zero is retained',
    (tester) async {
      await launch(tester);
      await typeAt(tester, 1, '0');
      await tester.pump();
      expect(value(tester, 1), '0');
      expect(focused(tester, 2), true);
      await typeAt(tester, 2, 'abc');
      await tester.pump();
      expect(value(tester, 2), '');
      expect(focused(tester, 2), true);
      await typeAt(tester, 2, '2');
      await tester.pump();
      expect(focused(tester, 3), true);
      await tester.tap(digit(1));
      await tester.pump();
      await typeAt(tester, 1, '9');
      await tester.pump();
      expect(value(tester, 1), '9');
      expect(value(tester, 2), '2');
      await finish(tester);
    },
  );

  testWidgets(
    'soft-keyboard deletion from an empty box clears the preceding digit',
    (tester) async {
      await launch(tester);
      await typeAt(tester, 1, '4');
      await tester.pump();
      expect(focused(tester, 2), true);
      // A mobile IME deletes the preceding digit in the real buffer, without a key event.
      tester.testTextInput.updateEditingValue(
        const TextEditingValue(
          text: '',
          selection: TextSelection.collapsed(offset: 0),
        ),
      );
      await tester.pump();
      expect(value(tester, 1), '');
      expect(focused(tester, 1), true);
      await typeAt(tester, 1, '');
      await tester.pump();
      expect(focused(tester, 1), true);
      await finish(tester);
    },
  );

  testWidgets(
    'hardware Backspace clears a filled digit, then the preceding digit',
    (tester) async {
      await launch(tester);
      await typeAt(tester, 1, '12');
      await tester.pump();
      await tester.tap(digit(2));
      await tester.pump();
      await tester.sendKeyEvent(LogicalKeyboardKey.backspace);
      await tester.pump();
      expect(value(tester, 2), '');
      expect(value(tester, 1), '1');
      await tester.sendKeyEvent(LogicalKeyboardKey.backspace);
      await tester.pump();
      expect(value(tester, 1), '');
      expect(focused(tester, 1), true);
      await finish(tester);
    },
  );

  testWidgets(
    'mixed-text paste fills all six boxes, including from a later box',
    (tester) async {
      await launch(tester);
      await tester.tap(digit(4));
      await tester.pump();
      await typeAt(tester, 4, 'Your code is 482913');
      await tester.pump();
      expect(List.generate(6, (i) => value(tester, i + 1)).join(), '482913');
      expect(tester.widget<FilledButton>(submit).onPressed, isNotNull);
      await finish(tester);
    },
  );

  testWidgets('123456 succeeds only after Submit and announces status', (
    tester,
  ) async {
    final handle = tester.ensureSemantics();
    await launch(tester);
    await typeAt(tester, 1, '123456');
    await tester.pump();
    expect(find.text("You're in."), findsNothing);
    await tester.tap(submit);
    await tester.pump();
    expect(find.text("You're in."), findsOneWidget);
    expect(
      tester
          .getSemantics(find.byKey(const ValueKey('otp_status')))
          .getSemanticsData()
          .flagsCollection
          .isLiveRegion,
      true,
    );
    handle.dispose();
    await finish(tester);
  });

  testWidgets(
    'incorrect code shows exact error, clears every box and restores first focus',
    (tester) async {
      await launch(tester);
      await typeAt(tester, 1, '482913');
      await tester.pump();
      await tester.tap(submit);
      await tester.pump();
      expect(find.text("That code didn't work. Try again."), findsOneWidget);
      for (var n = 1; n <= 6; n++) {
        expect(value(tester, n), '');
      }
      expect(focused(tester, 1), true);
      expect(tester.widget<FilledButton>(submit).onPressed, isNull);
      await typeAt(tester, 1, '1');
      await tester.pump();
      expect(find.text("That code didn't work. Try again."), findsNothing);
      await finish(tester);
    },
  );

  testWidgets(
    'resend is disabled for 30 seconds and restarts a full cooldown',
    (tester) async {
      var now = DateTime.utc(2026);
      await launch(tester, now: () => now);
      expect(find.text('Resend in 0:30'), findsOneWidget);
      expect(tester.widget<TextButton>(resend).onPressed, isNull);
      now = now.add(const Duration(seconds: 29));
      await tester.pump(const Duration(seconds: 29));
      expect(find.text('Resend in 0:01'), findsOneWidget);
      expect(tester.widget<TextButton>(resend).onPressed, isNull);
      now = now.add(const Duration(seconds: 1));
      await tester.pump(const Duration(seconds: 1));
      expect(tester.widget<TextButton>(resend).onPressed, isNotNull);
      await tester.tap(resend);
      await tester.pump();
      expect(find.text('Resend in 0:30'), findsOneWidget);
      expect(tester.widget<TextButton>(resend).onPressed, isNull);
      await finish(tester);
    },
  );

  testWidgets('resend uses elapsed time after returning from the background', (
    tester,
  ) async {
    var now = DateTime.utc(2026);
    await launch(tester, now: () => now);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.paused);
    now = now.add(const Duration(minutes: 1));
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.hidden);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.inactive);
    tester.binding.handleAppLifecycleStateChanged(AppLifecycleState.resumed);
    await tester.pump();
    expect(tester.widget<TextButton>(resend).onPressed, isNotNull);
    await finish(tester);
  });

  testWidgets(
    'six screen-reader fields expose clean labels/values and support editing',
    (tester) async {
      final handle = tester.ensureSemantics();
      await launch(tester);
      for (var n = 1; n <= 6; n++) {
        final data = tester.getSemantics(digit(n)).getSemanticsData();
        expect(data.label, 'Digit $n of 6');
        expect(data.value, '');
        expect(data.flagsCollection.isTextField, true);
        expect(data.hasAction(SemanticsAction.setText), true);
      }
      await typeAt(tester, 1, '123');
      await tester.pump();
      final node = tester.getSemantics(digit(3));
      tester.binding.performSemanticsAction(
        SemanticsActionEvent(
          type: SemanticsAction.setText,
          viewId: tester.view.viewId,
          nodeId: node.id,
          arguments: '7',
        ),
      );
      await tester.pump();
      expect(value(tester, 3), '7');
      expect(focused(tester, 4), true);
      expect(find.bySemanticsLabel('Welcome back'), findsNothing);
      handle.dispose();
      await finish(tester);
    },
  );

  testWidgets('system dark mode switches the supplied palette and logo', (
    tester,
  ) async {
    await launch(tester, mode: ThemeMode.system);
    tester.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
    addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
    await tester.pumpAndSettle();
    final title = tester.widget<Text>(find.text('Enter your code'));
    expect(title.style!.color, colors.dark['text_primary']);
    expect(
      find.byWidgetPredicate(
        (w) =>
            w is Image &&
            w.image is AssetImage &&
            (w.image as AssetImage).assetName ==
                'assets/thamani-logo-white.png',
      ),
      findsOneWidget,
    );
    await finish(tester);
  });

  testWidgets('small screen, large text and keyboard keep content scrollable', (
    tester,
  ) async {
    await launch(tester);
    tester.view.physicalSize = const Size(320, 640);
    tester.platformDispatcher.textScaleFactorTestValue = 2;
    tester.view.viewInsets = const FakeViewPadding(bottom: 260);
    addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
    addTearDown(tester.view.resetViewInsets);
    await tester.pump();
    expect(tester.takeException(), isNull);
    await tester.ensureVisible(submit);
    await tester.pump();
    expect(tester.getRect(submit).bottom, lessThanOrEqualTo(380));
    expect(tester.takeException(), isNull);
    await finish(tester);
  });
}
