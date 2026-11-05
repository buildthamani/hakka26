package ke.droidcon.hakka.ui.theme

import android.content.res.AssetManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject

/** Colours for the OTP screen, read from `assets/colours.json`. */
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
        private const val FILE = "colours.json"

        fun fromAssets(assets: AssetManager, dark: Boolean): OtpColors {
            val json = assets.open(FILE).bufferedReader().use { it.readText() }
            val palette = JSONObject(json)
                .getJSONObject("otp")
                .getJSONObject(if (dark) "dark" else "light")

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
    }
}

val LocalOtpColors = staticCompositionLocalOf<OtpColors> {
    error("OtpColors not provided; wrap content in HakkaTheme")
}

@Composable
fun HakkaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val otpColors = remember(darkTheme) { OtpColors.fromAssets(context.assets, darkTheme) }
    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        CompositionLocalProvider(LocalOtpColors provides otpColors, content = content)
    }
}
