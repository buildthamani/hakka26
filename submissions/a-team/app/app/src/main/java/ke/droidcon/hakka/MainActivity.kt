package ke.droidcon.hakka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import ke.droidcon.hakka.otp.ColourTokens
import ke.droidcon.hakka.otp.LocalOtpColors
import ke.droidcon.hakka.otp.OtpColors
import ke.droidcon.hakka.otp.OtpScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HakkaTheme {
                OtpScreen()
            }
        }
    }
}

/** Provides the light or dark palette from `assets/colours.json`, following the system theme. */
@Composable
fun HakkaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val json = remember { context.assets.open("colours.json").bufferedReader().use { it.readText() } }
    val colors = remember(json, darkTheme) {
        OtpColors.fromTokens(ColourTokens.parse(json, if (darkTheme) ColourTokens.DARK else ColourTokens.LIGHT))
    }
    val scheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = scheme.copy(
            primary = colors.buttonBg,
            onPrimary = colors.buttonText,
            surface = colors.sheet,
            onSurface = colors.textPrimary,
            background = colors.sheet,
            onBackground = colors.textPrimary,
            error = colors.error,
        ),
    ) {
        CompositionLocalProvider(LocalOtpColors provides colors, content = content)
    }
}

@Preview(name = "Light")
@Composable
private fun OtpScreenLightPreview() {
    HakkaTheme(darkTheme = false) { OtpScreen() }
}

@Preview(name = "Dark")
@Composable
private fun OtpScreenDarkPreview() {
    HakkaTheme(darkTheme = true) { OtpScreen() }
}
