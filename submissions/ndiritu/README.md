# Issue 1 — Android Compose

Implementation is in `app/`. The supplied template build versions are preserved.
The light and dark palettes come from `assets/colours.json`; both supplied logos are bundled.

Run `./gradlew assembleDebug testDebugUnitTest` from `app/`.
Run `./gradlew connectedDebugAndroidTest` with an Android device or emulator.

Validation in this workspace:
- Nine reducer/countdown JUnit tests passed using the cached Kotlin 2.4.20 compiler and JUnit 4.13.2 directly.
- Four Compose instrumentation tests are provided but could not run: the sandbox blocks ADB's server socket.
- Full Gradle build could not run: Gradle 9.7.0 is not cached and services.gradle.org DNS resolution failed.
- Runtime screenshots and demo recording have not been captured; visual matching remains unverified on-device.

`PROMPT.md` contains the original user prompt verbatim. Team identity, screenshot and demo submission artifacts still need to be supplied before a competition PR.
