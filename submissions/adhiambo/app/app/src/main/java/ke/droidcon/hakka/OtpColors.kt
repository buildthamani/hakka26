package ke.droidcon.hakka

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf

/** Exact OTP palette from assets/colours.json. */
data class OtpColors(
    val sheet: Color, val scrim: Color, val text: Color, val secondary: Color,
    val filled: Color, val filledText: Color, val focused: Color, val outline: Color,
    val empty: Color, val emptyOutline: Color, val placeholder: Color,
    val button: Color, val buttonText: Color, val disabled: Color, val link: Color,
    val success: Color, val error: Color,
) {
    companion object {
        val Light = OtpColors(
            Color(0xFFFFFFFF), Color(0x99000000), Color(0xFF111111), Color(0xFF5F6368),
            Color(0xFF12361B), Color.White, Color.White, Color(0xFF111111),
            Color(0xFFF1F1F1), Color(0xFFE0E0E0), Color(0xFF9E9E9E),
            Color(0xFF12361B), Color.White, Color(0xFFC9D1CB), Color(0xFF12361B),
            Color(0xFF1B7F3A), Color(0xFFC62828),
        )
        val Dark = OtpColors(
            Color(0xFF161A17), Color(0xCC000000), Color(0xFFF5F5F5), Color(0xFFB0B5B1),
            Color(0xFF2E7D45), Color.White, Color(0xFF161A17), Color(0xFFF5F5F5),
            Color(0xFF262B27), Color(0xFF353B36), Color(0xFF6F7570),
            Color(0xFF2E7D45), Color.White, Color(0xFF2A302B), Color(0xFF7BC795),
            Color(0xFF7BC795), Color(0xFFEF9A9A),
        )
    }
}

/** Provided explicitly by HakkaTheme, independently of Material color overrides. */
internal val LocalOtpColors = staticCompositionLocalOf { OtpColors.Light }
