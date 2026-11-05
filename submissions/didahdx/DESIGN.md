# OTP screen design

Visual authority: repository `reference/otp-reference.png`, with exact issue #1 copy and the supplied `assets/colours.json` taking precedence.

Mode: Operate. The only task is entering and verifying an OTP. Preserve the reference composition rather than adding a separate visual identity.

- Dimmed background and centered, rounded bottom sheet, bounded to 480 dp on wide screens.
- Logo, title, two-line subtitle, six circular digits, resend control, flexible whitespace and full-width pill Submit button.
- Filled digits use the supplied green/white palette; focus uses the outlined surface; empty digits use grey with a dash placeholder. All colours resolve from the light/dark JSON tokens.
- The issue's logo asset differs from the illustration in the reference. Use the supplied logo. The incomplete Submit button uses the supplied disabled palette even though the reference shows green.
- Typography uses native scalable Android text. Title 26 sp, body/button 14 sp, resend 13 sp, digits 18 sp.
- Keyboard-visible layout reduces top spacing and logo size, scrolls the input row into view, and retains a fixed Submit action and verification feedback. System insets are respected.
- Inputs are real accessible Android text fields hosted in Compose, with numbered labels and full clipboard support. Status changes use a polite live region.
