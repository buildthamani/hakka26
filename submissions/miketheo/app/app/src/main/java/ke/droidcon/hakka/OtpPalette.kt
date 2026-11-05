package ke.droidcon.hakka

import androidx.compose.ui.graphics.Color

data class OtpPalette(
    val sheet: Color,
    val scrim: Color,
    val primary: Color,
    val secondary: Color,
    val filled: Color,
    val empty: Color,
    val outline: Color,
    val placeholder: Color,
    val disabled: Color,
    val link: Color,
    val success: Color,
    val error: Color,
)

val LightOtp = OtpPalette(
    sheet = Color(0xFFFFFFFF),
    scrim = Color(0x99000000),
    primary = Color(0xFF111111),
    secondary = Color(0xFF5F6368),
    filled = Color(0xFF12361B),
    empty = Color(0xFFF1F1F1),
    outline = Color(0xFFE0E0E0),
    placeholder = Color(0xFF9E9E9E),
    disabled = Color(0xFFC9D1CB),
    link = Color(0xFF12361B),
    success = Color(0xFF1B7F3A),
    error = Color(0xFFC62828),
)

val DarkOtp = OtpPalette(
    sheet = Color(0xFF161A17),
    scrim = Color(0xCC000000),
    primary = Color(0xFFF5F5F5),
    secondary = Color(0xFFB0B5B1),
    filled = Color(0xFF2E7D45),
    empty = Color(0xFF262B27),
    outline = Color(0xFF353B36),
    placeholder = Color(0xFF6F7570),
    disabled = Color(0xFF2A302B),
    link = Color(0xFF7BC795),
    success = Color(0xFF7BC795),
    error = Color(0xFFEF9A9A),
)
