package ke.droidcon.hakka.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun OtpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colours = remember(context, darkTheme) {
        OtpColours.fromAssets(context, darkTheme)
    }

    val materialScheme = if (darkTheme) {
        darkColorScheme(
            primary = colours.buttonBackground,
            onPrimary = colours.buttonText,
            background = colours.sheet,
            onBackground = colours.textPrimary,
            surface = colours.sheet,
            onSurface = colours.textPrimary,
        )
    } else {
        lightColorScheme(
            primary = colours.buttonBackground,
            onPrimary = colours.buttonText,
            background = colours.sheet,
            onBackground = colours.textPrimary,
            surface = colours.sheet,
            onSurface = colours.textPrimary,
        )
    }

    CompositionLocalProvider(LocalOtpColours provides colours) {
        MaterialTheme(
            colorScheme = materialScheme,
            content = content,
        )
    }
}
