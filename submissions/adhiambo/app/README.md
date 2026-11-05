# Hackathon OTP screen

Android Jetpack Compose bottom sheet matching the challenge's copy, circular digit states, and `assets/colours.json` palette. The logo is the supplied `assets/thamani-logo.png` (the challenge requires that asset even though the reference illustration uses a different logo).

`OtpState` holds pure input, deletion, validation, and countdown transitions. `OtpInput` implements the six accessible circular fields. Empty fields contain actual empty strings; `EmptyFieldBackspace` uses Compose platform input interception to handle backward-deletion requests from software keyboards. `HakkaTheme` explicitly provides `LocalOtpColors`, independently of Material background colors. `OtpScreen` presents the sheet, retry control, live status, and submit button. State survives activity recreation; the countdown uses elapsed time so backgrounding does not pause it.

- Mixed-text paste extracts ASCII digits and drops overflow beyond six positions.
- Backspace clears a filled current box; on an empty box it moves back and clears the previous digit.
- `123456` shows `You're in.`. Other complete codes show `That code didn't work. Try again.`, clear the fields, and refocus the first.
- The 30-second resend cooldown starts on entry and restarts after each resend.
- Test IDs are `otp_digit_1` through `otp_digit_6`, `otp_submit`, `otp_resend`, and `otp_status`.

## Verification

Run from this directory with JDK 21 and an Android SDK configured:

```sh
./gradlew assembleDebug testDebugUnitTest --no-daemon
./gradlew lintDebug
```

CI runs the JVM tests, including all pure OTP transitions. With an emulator/device connected, run the Compose UI tests too:

```sh
./gradlew connectedDebugAndroidTest
```

The UI suite covers real clipboard paste, focus, visible digits, hardware deletion and real IME deletion requests (including code-point deletion), success/error behavior, countdown/retry, state restoration, palette states, and dark-mode accessibility.
