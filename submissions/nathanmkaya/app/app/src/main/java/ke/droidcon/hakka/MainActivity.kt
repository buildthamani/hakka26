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
import ke.droidcon.hakka.otp.LocalOtpColors
import ke.droidcon.hakka.otp.OtpScreen
import ke.droidcon.hakka.otp.loadOtpColors

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

@Composable
fun HakkaTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val assets = LocalContext.current.assets
    val otpColors = remember(assets, darkTheme) { loadOtpColors(assets, darkTheme) }
    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        CompositionLocalProvider(LocalOtpColors provides otpColors, content = content)
    }
}
