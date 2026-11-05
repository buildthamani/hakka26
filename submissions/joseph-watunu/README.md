# OTP verification

Flutter implementation of issue #1. Run from `app/`:

```sh
flutter pub get
flutter build apk --debug && flutter test
flutter run
```

The verification code is `123456`, as specified by the challenge. Other complete
codes show the exact error message, clear the input, and restore the first digit.

- Six circular cells share one native numeric editing session for reliable
  backspace, selection, clipboard paste, and one-time-code autofill.
- Tap a filled cell to replace its digit. Long press any cell and choose Paste
  to extract a code from an SMS such as `Your code is 482913`.
- Resend starts disabled for 30 seconds and uses a deadline that remains correct
  after backgrounding. Tapping Resend resets the input and countdown.
- Each cell has its own editable screen-reader label and test identifier.
  Status changes use a live region. Buttons also expose the issue's identifiers.
- Both themes load the supplied `colours.json` and use the supplied Thamani logos.
  The sheet scrolls when the keyboard or large text reduces the available space.

The tests cover all ten criteria, native editing updates and autofill messages,
clipboard paste, lifecycle timing, semantics, both palettes, and compact layouts.

`screenshot.png` shows the partially filled state. `screenshot-dark.png` shows
the same state in dark mode. `demo.mp4` records the running Android app.
