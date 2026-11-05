# Reviewed OTP baselines

The reference PNG guides layout and typography. Issue #1 and the supplied assets
take precedence where the reference differs:

- Use the supplied Thamani logo, not the flower shown in the reference; dark mode
  uses the supplied white variant.
- Use the exact issue title, subtitle and status messages.
- Empty boxes show dashes, never actual zero digits.
- Partially filled input keeps Submit disabled and shows the initial resend cooldown.
- Apply the light/dark OTP palette from colours.json.

Goldens capture the actual widget at 390 × 844 logical pixels, pixel ratio 1,
with the bundled Roboto font and a frozen clock. Two digits are filled, the third
box is focused and empty, and the remaining boxes are empty.

Render review candidates with:

    flutter test test/golden_test.dart --dart-define=CAPTURE_RENDERS=true

Inspect test/previews/ against the design before updating baselines. A passing
golden is a regression check, not proof of similarity to the reference PNG.

Golden comparisons allow up to 5% changed pixels across platforms. Larger
differences still fail and produce the standard comparison images.
