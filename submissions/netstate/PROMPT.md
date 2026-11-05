Design a mobile OTP Verification screen (390×844, iPhone viewport) for a fintech app. Clean, minimal, white background, lots of breathing room.

LAYOUT (top to bottom, centered, single column):
1. Top bar: small back arrow (←) at top-left, 16px from the edge.
2. Hero illustration: a line-art smartphone with a chat/message bubble overlapping its top-right corner. Stroke-only style, 2px strokes, with a small ground line beneath the phone. Add a few tiny decorative accents (small circles, dots, a sparkle) scattered around it. Primary color for the strokes, with one small yellow accent dot.
3. Title: "OTP Verification" — 20px, semibold, dark gray/near-black, centered.
4. Greeting: "Hello {userName}," — 13px, medium weight, centered.
5. Helper text: "Thank you for registering with us. Please type the OTP as shared on your mobile {maskedPhone}" — 12px, muted gray (#8A8A8A), centered, max-width ~280px, line-height 1.5. Phone masked like XXXXXXX123.
6. OTP input: 4 separate square boxes in a row, each 52×52px, 1px light gray border (#D9D9D9), 8px radius, 12px gap, large centered digit (22px, medium). Focus state: border switches to the primary color with a subtle 3px glow. Filled state: darker border. Auto-advance to the next box on input, backspace moves back, supports paste of the full code.
7. Resend row: "OTP not received? RESEND" — 11px muted gray, with "RESEND" in the primary color, semibold, tappable. After tapping, disable it and show a 30s countdown ("Resend in 0:29").
8. Submit button: pinned near the bottom, full-width (24px side margins), 48px tall, 8px radius, primary color fill, white "Submit" label (15px, medium). Disabled state (reduced opacity) until all 4 digits are entered.

VARIANTS:
- Variant A (brand): primary color #12873F (green), green line illustration.
- Variant B (neutral): primary color #000000, black line illustration where the phone shows a "••••" password field and a padlock instead of the chat bubble.

STYLE:
- Font: Inter or Poppins. No gradients, no heavy shadows. Flat, airy, modern.
- Screen has a very soft drop shadow so it reads as a card on a light gray canvas.
- Touch targets at least 44px.

STATES TO SHOW:
- Default (empty), focused, filled, error (red border #E5484D on all boxes plus "Invalid OTP, try again" below the boxes with a small shake animation), and success.

ACCESSIBILITY:
- Each box has an aria-label ("Digit 1 of 4"), numeric keypad on mobile (inputmode="numeric", autocomplete="one-time-code"), color contrast of at least 4.5:1 for text.

OUTPUT: [pick one: Figma-style design / React + Tailwind component / HTML+CSS / Flutter widget]. Make it fully responsive and copy-paste ready, with the digit inputs and resend timer actually working.