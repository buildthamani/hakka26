# Local verification

Verified on 2026-11-05 using the Pixel 2 API 35 emulator (Android 15, 1080×1920).

- `./gradlew assembleDebug testDebugUnitTest connectedDebugAndroidTest --no-daemon`: passed.
- 12 JVM unit tests passed (11 OTP tests plus the template's AppInfo test).
- 4 emulator tests passed: typing/deletion/success; real clipboard paste/error/reset; IME deletion/activity recreation; rapid hardware key entry without intervening recomposition.
- Real captures checked in light mode, dark mode, with the keyboard visible, and at 130% system font scale.
- `screenshot.png` is a partially filled capture of the running app. `demo.mp4` is an Android screen recording of invalid and valid submissions.
- Repository submission validator passed. All deliverable files are inside `submissions/didahdx/`. PROMPT.md is unchanged, 557 characters after trimming its trailing newline.

The build commands match CI, but remote GitHub CI and official grading have not been run. Screenshot differences required by the challenge include the supplied logo, exact issue copy, dash placeholders and the disabled button palette. No real SMS service is connected.
