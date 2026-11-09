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
import androidx.compose.ui.tooling.preview.Preview

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
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OtpScreenPreview() {
    HakkaTheme {
        OtpScreen(
            initialState = OtpState(
                digits = listOf("2", "1", "5", "", "", ""),
                focusedIndex = 3,
            ),
        )
    }
}
