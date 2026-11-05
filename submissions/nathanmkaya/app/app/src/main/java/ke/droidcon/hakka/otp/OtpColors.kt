package ke.droidcon.hakka.otp

import android.content.res.AssetManager
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/** The `otp` palette from `assets/colours.json`, one instance per light or dark theme. */
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
)

val LocalOtpColors = staticCompositionLocalOf<OtpColors> { error("OtpColors are provided by HakkaTheme") }

fun loadOtpColors(assets: AssetManager, dark: Boolean): OtpColors {
    val json = assets.open("colours.json").bufferedReader().use { it.readText() }
    val palette = JSONObject(json).getJSONObject("otp").getJSONObject(if (dark) "dark" else "light")
    fun color(key: String) = Color(android.graphics.Color.parseColor(palette.getString(key)))
    return OtpColors(
        sheet = color("sheet"),
        scrim = color("scrim"),
        textPrimary = color("text_primary"),
        textSecondary = color("text_secondary"),
        digitFilledBg = color("digit_filled_bg"),
        digitFilledText = color("digit_filled_text"),
        digitFocusedBg = color("digit_focused_bg"),
        digitFocusedOutline = color("digit_focused_outline"),
        digitEmptyBg = color("digit_empty_bg"),
        digitEmptyOutline = color("digit_empty_outline"),
        digitPlaceholder = color("digit_placeholder"),
        buttonBg = color("button_bg"),
        buttonText = color("button_text"),
        buttonDisabledBg = color("button_disabled_bg"),
        link = color("link"),
        success = color("success"),
        error = color("error"),
    )
}
