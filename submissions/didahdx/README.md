# DidahDx — OTP verification

Native Android Kotlin/Jetpack Compose submission for issue #1.

## Build and test

From `app/`, with Java 21 and the Android SDK configured:

```sh
./gradlew assembleDebug testDebugUnitTest --no-daemon
./gradlew connectedDebugAndroidTest --no-daemon
```

The second command requires an emulator or connected device. CI runs the first command's two tasks. The APK is `app/app/build/outputs/apk/debug/app-debug.apk`. Local SDK paths and generated build files are ignored by Git.

## Structure

- `MainActivity.kt`: Android entry point only.
- `OtpScreen.kt`: sheet layout and lifecycle-aware state collection.
- `OtpDigitField.kt`: reusable circular input, including the native IME/clipboard bridge.
- `OtpViewModel.kt`: one state owner, actions and resend deadline.
- `OtpLogic.kt`: immutable state and pure, unit-tested input/verification rules.
- `OtpTheme.kt`: reads both palettes directly from the supplied `colours.json`.

The small native EditText bridge handles soft-keyboard deletion on an empty field and complete clipboard text before numeric filtering. It remains a real accessible editable field; each field is labelled “Digit N of 6”. Compose test tags identify the six wrappers and all required controls.

## Input and lifecycle decisions

ASCII digits are accepted; `0` is a valid entered digit, while empty boxes show a dash. A paste with six or more digits replaces the whole code and takes the first six digits. A shorter paste begins at the focused box and truncates at the last box. Non-digits alone do nothing. Backspace clears a filled box, or moves back and clears the previous digit when empty. Editing clears old verification feedback.

The resend deadline uses Android's monotonic elapsed-time clock. Rotation retains input, focus and deadline through the ViewModel. Resuming recomputes remaining time immediately. Process death starts a fresh local challenge; OTP values are not written to disk or logs. The local success code is `123456` as explicitly required by the issue. No real SMS is sent.

## Visual authority

`reference/otp-reference.png` defines the composition; issue #1 overrides its copy and zero placeholders. Supplied assets and light/dark colour tokens are used. The keyboard opens on launch; hide it with Android Back to inspect the full-height sheet. The content scrolls independently of the Submit button when keyboard, landscape or larger fonts reduce available height.

## Official guidance

- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Compose state hoisting](https://developer.android.com/develop/ui/compose/state-hoisting)
- [Views in Compose](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose)
- [Compose testing](https://developer.android.com/develop/ui/compose/testing)

`PROMPT.md` is preserved verbatim from the user's saved prompt. Screenshot and demo are captures of the running Android app, not design mockups.
