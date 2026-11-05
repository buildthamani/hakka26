package ke.droidcon.hakka.otp

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject
import android.graphics.Color as AndroidColor

/** OTP palette, read from `assets/colours.json` (`otp.light` / `otp.dark`). */
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
        private const val ASSET = "colours.json"

        fun load(context: Context, dark: Boolean): OtpColors {
            val json = context.assets.open(ASSET).bufferedReader().use { it.readText() }
            val palette = JSONObject(json).getJSONObject("otp").getJSONObject(if (dark) "dark" else "light")
            fun color(key: String) = Color(AndroidColor.parseColor(palette.getString(key)))
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
    }
}

/** Palette for the current system theme; switches automatically in dark mode. */
@Composable
fun rememberOtpColors(dark: Boolean = isSystemInDarkTheme()): OtpColors {
    val context = LocalContext.current
    return remember(dark) { OtpColors.load(context, dark) }
}
