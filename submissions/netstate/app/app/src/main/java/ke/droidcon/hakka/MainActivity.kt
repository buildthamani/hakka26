package ke.droidcon.hakka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HakkaTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
fun HakkaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

@Composable
fun HomeScreen() {
    var showOtp by remember { mutableStateOf(false) }
    val reducer = remember { ke.droidcon.hakka.otp.OtpReducer() }
    val state by reducer.state.collectAsState()

    if (showOtp) {
        ke.droidcon.hakka.otp.OtpScreen(
            state = state,
            onDigitsChanged = reducer::onDigitsChanged,
            onSubmit = reducer::onSubmit,
            onResend = reducer::onResend,
            onTick = reducer::onTick,
            onBack = { showOtp = false }
        )
    } else {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.thamani_logo),
                    contentDescription = "Thamani logo",
                    modifier = Modifier.size(64.dp),
                )
                Text(AppInfo.NAME, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { showOtp = true }) {
                    Text("Login")
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    HakkaTheme { HomeScreen() }
}
