package ke.droidcon.hakka

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import hakka.composeapp.generated.resources.Res
import ke.droidcon.hakka.otp.DemoOtpVerifier
import ke.droidcon.hakka.otp.OtpScreen
import ke.droidcon.hakka.otp.OtpState
import ke.droidcon.hakka.theme.OtpPalette
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    val dark = isSystemInDarkTheme()
    val palette by produceState<OtpPalette?>(initialValue = null) {
        value = OtpPalette.parse(Res.readBytes("files/colours.json").decodeToString())
    }
    val state = remember { OtpState(verify = DemoOtpVerifier::verify) }

    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        palette?.let { OtpScreen(state, it.forTheme(dark)) }
    }
}
