package ke.droidcon.hakka

import androidx.compose.ui.graphics.Color

/** Palette from assets/colours.json ("otp" block). */
internal class OtpColors(
    val sheet: Color, val scrim: Color, val textPrimary: Color, val textSecondary: Color,
    val filledBg: Color, val filledText: Color, val focusedBg: Color, val focusedOutline: Color,
    val emptyBg: Color, val emptyOutline: Color, val placeholder: Color,
    val buttonBg: Color, val buttonText: Color, val buttonDisabledBg: Color,
    val link: Color, val success: Color, val error: Color,
)

internal val LightColors = OtpColors(
    Color(0xFFFFFFFF), Color(0x99000000), Color(0xFF111111), Color(0xFF5F6368),
    Color(0xFF12361B), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF111111),
    Color(0xFFF1F1F1), Color(0xFFE0E0E0), Color(0xFF9E9E9E),
    Color(0xFF12361B), Color(0xFFFFFFFF), Color(0xFFC9D1CB),
    Color(0xFF12361B), Color(0xFF1B7F3A), Color(0xFFC62828),
)

internal val DarkColors = OtpColors(
    Color(0xFF161A17), Color(0xCC000000), Color(0xFFF5F5F5), Color(0xFFB0B5B1),
    Color(0xFF2E7D45), Color(0xFFFFFFFF), Color(0xFF161A17), Color(0xFFF5F5F5),
    Color(0xFF262B27), Color(0xFF353B36), Color(0xFF6F7570),
    Color(0xFF2E7D45), Color(0xFFFFFFFF), Color(0xFF2A302B),
    Color(0xFF7BC795), Color(0xFF7BC795), Color(0xFFEF9A9A),
)

