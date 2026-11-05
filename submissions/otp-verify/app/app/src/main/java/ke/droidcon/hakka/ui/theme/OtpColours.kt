package ke.droidcon.hakka.ui.theme

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/**
 * The design tokens from app/src/main/assets/colours.json. They are loaded from the
 * bundled asset instead of being duplicated as Kotlin constants, so the screen always
 * uses the supplied palette for both light and dark themes.
 */
@Immutable
data class OtpColours(
    val isDark: Boolean,
    val sheet: Color,
    val scrim: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val digitFilledBackground: Color,
    val digitFilledText: Color,
    val digitFocusedBackground: Color,
    val digitFocusedOutline: Color,
    val digitEmptyBackground: Color,
    val digitEmptyOutline: Color,
    val digitPlaceholder: Color,
    val buttonBackground: Color,
    val buttonText: Color,
    val buttonDisabledBackground: Color,
    val link: Color,
    val success: Color,
    val error: Color,
) {
    companion object {
        fun fromAssets(context: Context, darkTheme: Boolean): OtpColours {
            val json = context.assets
                .open("colours.json")
                .bufferedReader()
                .use { it.readText() }
            val mode = JSONObject(json)
                .getJSONObject("otp")
                .getJSONObject(if (darkTheme) "dark" else "light")

            fun colour(key: String): Color =
                Color(AndroidColor.parseColor(mode.getString(key)))

            return OtpColours(
                isDark = darkTheme,
                sheet = colour("sheet"),
                scrim = colour("scrim"),
                textPrimary = colour("text_primary"),
                textSecondary = colour("text_secondary"),
                digitFilledBackground = colour("digit_filled_bg"),
                digitFilledText = colour("digit_filled_text"),
                digitFocusedBackground = colour("digit_focused_bg"),
                digitFocusedOutline = colour("digit_focused_outline"),
                digitEmptyBackground = colour("digit_empty_bg"),
                digitEmptyOutline = colour("digit_empty_outline"),
                digitPlaceholder = colour("digit_placeholder"),
                buttonBackground = colour("button_bg"),
                buttonText = colour("button_text"),
                buttonDisabledBackground = colour("button_disabled_bg"),
                link = colour("link"),
                success = colour("success"),
                error = colour("error"),
            )
        }
    }
}

val LocalOtpColours = staticCompositionLocalOf<OtpColours> {
    error("OtpColours are not available. Wrap the screen in OtpTheme.")
}
