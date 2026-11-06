# Issue 1 — Android Compose

Implementation is in `app/`. The supplied template build versions are preserved.
The light and dark palettes come from `assets/colours.json`. The flower is a native vector matching the reference; the supplied brand assets remain bundled.

Run `./gradlew assembleDebug testDebugUnitTest` from `app/`.
Run `./gradlew connectedDebugAndroidTest` with an Android device or emulator.

`MainActivity.kt` only hosts the screen. `OtpScreen.kt` contains the sheet and small
heading/resend composables; `OtpState.kt` is the single saved source of digit and
requested-focus state. `OtpInput.kt` observes each field's focus interaction for
styling without copying focus into another mutable state.

`OtpKeyboardInput.kt` routes physical and software backspace to one callback,
forwarding all ordinary text edits to Compose. There is no invisible anchor or transformed
sentinel text. Device tests cover both hardware and software keyboard deletion.

Submission media: `screenshot.png` shows three filled circles, the white focused
fourth circle, and two empty circles. `demo.mp4` is a short emulator recording.

`PROMPT.md` contains the original user prompt verbatim.
