package ke.droidcon.hakka

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject

data class OtpColors(private val values: Map<String, Color>) {
    operator fun get(key: String): Color = values.getValue(key)
}

@Composable
fun otpColors(): OtpColors {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    return remember(context, dark) {
        val json = context.assets.open("colours.json").bufferedReader().use { it.readText() }
        val colors = JSONObject(json).getJSONObject("otp").getJSONObject(if (dark) "dark" else "light")
        OtpColors(colors.keys().asSequence().associateWith { Color(android.graphics.Color.parseColor(colors.getString(it))) })
    }
}

@Composable
fun OtpTheme(content: @Composable () -> Unit) {
    val colors = otpColors()
    val scheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = scheme.copy(
        primary = colors["button_bg"], onPrimary = colors["button_text"],
        surface = colors["sheet"], onSurface = colors["text_primary"],
        error = colors["error"],
    ), content = content)
}
