package ke.droidcon.hakka.otp

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** OTP colours, one set per theme, read from `assets/colours.json` (`otp.light` / `otp.dark`). */
@Immutable
data class OtpColors(
    val sheet: Color,
    val scrim: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val digitFilledBg: Color,
    val digitFilledText: Color,
    val digitFocusedBg: Color,
    val digitFocusedOutline: Color,
    val digitEmptyBg: Color,
    val digitEmptyOutline: Color,
    val digitPlaceholder: Color,
    val buttonBg: Color,
    val buttonText: Color,
    val buttonDisabledBg: Color,
    val link: Color,
    val success: Color,
    val error: Color,
) {
    companion object {
        fun fromTokens(tokens: Map<String, Long>): OtpColors {
            fun c(key: String) = Color(tokens[key] ?: error("Missing colour token '$key'"))
            return OtpColors(
                sheet = c("sheet"),
                scrim = c("scrim"),
                textPrimary = c("text_primary"),
                textSecondary = c("text_secondary"),
                digitFilledBg = c("digit_filled_bg"),
                digitFilledText = c("digit_filled_text"),
                digitFocusedBg = c("digit_focused_bg"),
                digitFocusedOutline = c("digit_focused_outline"),
                digitEmptyBg = c("digit_empty_bg"),
                digitEmptyOutline = c("digit_empty_outline"),
                digitPlaceholder = c("digit_placeholder"),
                buttonBg = c("button_bg"),
                buttonText = c("button_text"),
                buttonDisabledBg = c("button_disabled_bg"),
                link = c("link"),
                success = c("success"),
                error = c("error"),
            )
        }
    }
}

val LocalOtpColors = staticCompositionLocalOf<OtpColors> { error("OtpColors not provided") }

/**
 * Minimal, dependency-free reader for the flat colour objects in `colours.json`.
 * Returns ARGB values keyed by token name for `otp.<mode>`.
 */
object ColourTokens {
    const val LIGHT = "light"
    const val DARK = "dark"

    private val entry = Regex("\"([a-z_]+)\"\\s*:\\s*\"#([0-9A-Fa-f]{8}|[0-9A-Fa-f]{6})\"")

    fun parse(json: String, mode: String): Map<String, Long> {
        val otpStart = json.indexOf("\"otp\"").also { require(it >= 0) { "No 'otp' section" } }
        val modeKey = json.indexOf("\"$mode\"", otpStart).also { require(it >= 0) { "No '$mode' colours" } }
        val open = json.indexOf('{', modeKey)
        val close = json.indexOf('}', open)
        return entry.findAll(json.substring(open, close)).associate { match ->
            val (name, hex) = match.destructured
            name to hexToArgb(hex)
        }
    }

    /** `RRGGBB` is treated as opaque; `AARRGGBB` keeps its alpha. */
    fun hexToArgb(hex: String): Long {
        val value = hex.toLong(16)
        return if (hex.length == 6) 0xFF000000L or value else value
    }
}
