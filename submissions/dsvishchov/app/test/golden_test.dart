import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hakka/main.dart';
import 'package:hakka/otp_palette.dart';
import 'tolerant_golden_comparator.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  late OtpColors colors;
  setUp(() {
    final original = goldenFileComparator;
    goldenFileComparator = TolerantGoldenComparator(
      Directory.current.uri.resolve('test/golden_test.dart'),
    );
    addTearDown(() => goldenFileComparator = original);
  });
  setUpAll(() async {
    colors = await OtpColors.load();
    final fonts = FontLoader('Roboto')
      ..addFont(rootBundle.load('assets/fonts/Roboto-Regular.ttf'))
      ..addFont(rootBundle.load('assets/fonts/Roboto-Medium.ttf'))
      ..addFont(rootBundle.load('assets/fonts/Roboto-Bold.ttf'));
    await fonts.load();
  });

  for (final mode in [ThemeMode.light, ThemeMode.dark]) {
    testWidgets('partially filled OTP ${mode.name} matches reviewed design', (
      tester,
    ) async {
      tester.view.physicalSize = const Size(390, 844);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final boundary = GlobalKey();
      await tester.pumpWidget(
        RepaintBoundary(
          key: boundary,
          child: HakkaApp(
            colors: colors,
            themeMode: mode,
            now: () => DateTime.utc(2026),
          ),
        ),
      );
      await tester.pumpAndSettle();
      // The supplied logos supersede the flower in the reference screenshot.
      final expectedLogo = mode == ThemeMode.dark
          ? 'assets/thamani-logo-white.png'
          : 'assets/thamani-logo.png';
      expect(
        tester
            .widgetList<Image>(find.byType(Image))
            .map((image) => (image.image as AssetImage).assetName),
        contains(expectedLogo),
      );
      for (final image in tester.widgetList<Image>(find.byType(Image))) {
        await tester.runAsync(
          () =>
              precacheImage(image.image, tester.element(find.byType(HakkaApp))),
        );
      }
      await tester.enterText(find.byKey(const ValueKey('otp_input')), '21');
      await tester.pumpAndSettle();
      final target = find.byKey(boundary);
      if (const bool.fromEnvironment('CAPTURE_RENDERS')) {
        final render =
            boundary.currentContext!.findRenderObject()!
                as RenderRepaintBoundary;
        await tester.runAsync(() async {
          final image = await render.toImage(pixelRatio: 1);
          final data = await image.toByteData(format: ui.ImageByteFormat.png);
          final output = File('test/previews/otp_partial_${mode.name}.png');
          await output.parent.create(recursive: true);
          await output.writeAsBytes(data!.buffer.asUint8List());
          image.dispose();
        });
      } else {
        await expectLater(
          target,
          matchesGoldenFile('goldens/otp_partial_${mode.name}.png'),
        );
      }
      await tester.pumpWidget(const SizedBox());
    });
  }
}
