# OTP demo

Android Jetpack Compose screen with six immediately masked numeric boxes, light/dark themes, clipboard paste, focus advancement, and backward deletion. Mixed-content paste is rejected in full; input beyond the sixth position is discarded. Delete clears the current box and focuses the previous box without erasing its value.

Submit requires six digits and displays a local demo confirmation. The first submission starts a 60-second resend cooldown. Later submissions do not reset it. Resend becomes available at expiry, clears the code, and starts another 60-second cooldown. State survives activity recreation, and the timer uses elapsed time so backgrounding does not extend it. No network requests are made.

## Verification

With JDK 17+ and an Android SDK configured, run:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Start an Android emulator or connect a device, then run the Compose UI suite:

```sh
./gradlew :app:connectedDebugAndroidTest
```

UI coverage includes masking, focus, typing, real clipboard paste, invalid input, overflow, hardware/software deletion, field replacement, submission gating, timer expiry/restart, saved-state restoration, and dark theme.
