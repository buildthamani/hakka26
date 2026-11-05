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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HakkaTheme { OtpScreen() } }
    }
}

@Composable
fun HakkaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) OtpColors.Dark else OtpColors.Light
    CompositionLocalProvider(LocalOtpColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorScheme(
                background = colors.sheet, primary = colors.button,
            ) else lightColorScheme(background = colors.sheet, primary = colors.button),
            content = content,
        )
    }
}
