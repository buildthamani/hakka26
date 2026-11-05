# Issue 1 — Android Compose

Implementation is in `app/`. The supplied template build versions are preserved.
The light and dark palettes come from `assets/colours.json`; both supplied logos are bundled.

Run `./gradlew assembleDebug testDebugUnitTest` from `app/`.
Run `./gradlew connectedDebugAndroidTest` with an Android device or emulator.

The OTP input is isolated in `OtpInput.kt`; `OtpState.kt` contains the reducer.
Focus requests are independent of focus notifications, so entering three digits
leaves the fourth, empty circle focused. The hidden editing anchor is confined to
the input component so software keyboards can report backspace on an empty box.

Submission media: `screenshot.png` shows three filled circles, the white focused
fourth circle, and two empty circles. `demo.mp4` is a short emulator recording.

`PROMPT.md` contains the original user prompt verbatim.
