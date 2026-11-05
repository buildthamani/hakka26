# Thamani OTP — Flutter

Run from `app/` with Flutter stable and an Android SDK:

```sh
flutter pub get
flutter test
flutter analyze
flutter build apk --debug
flutter run
```

Enter `123456` and tap Submit for success. Other complete codes show the specified
error, clear the input and focus its first box. This is the local verification
behavior requested in Issue #1; resend restarts the 30-second local cooldown.

Six independently editable fields support number keyboards, digit replacement,
mixed-text paste and both hardware/IME backspace. One TextEditingController stores the code and selection; the six cells
render that buffer, without sentinel characters or mirrored controllers. Accessibility exposes only actual digits, with “Digit N of
6” labels and set-text actions; status is a live region and countdown ticks are not.
Each required test ID is both a ValueKey and a Semantics identifier.

The interface follows the reference layout, with intentional corrections required
by the issue: supplied Thamani logos, exact issue copy, dash placeholders, disabled
partial-code Submit and supplied light/dark colors. Large text can use two rows of
digits, and the sheet scrolls when the keyboard reduces available height.

There are 15 tests, including real editing updates, hardware Backspace, paste,
submit/reset, resend boundaries and background/resume, accessible editing, system
dark mode, small screens with large text/keyboard, and two reviewed golden images.
See `app/test/goldens/README.md` for golden capture and review instructions.

Verified locally with Flutter 3.44.7: all tests pass, analyzer reports no issues,
and the debug APK builds. Android accessibility nodes were inspected on Pixel_9a.
Linux CI and a human TalkBack/VoiceOver listening pass have not been verified.

The Gradle version is unchanged; its binary distribution avoids downloading the
optional sources/documentation in the larger all distribution.

The input keeps a contiguous code: selecting a later empty cell uses the first
empty position; deleting a middle digit shifts the suffix left. Golden tests allow
up to 5% changed pixels. After this refactor, all 15 tests and analysis pass locally;
the APK was not rebuilt for this update.
