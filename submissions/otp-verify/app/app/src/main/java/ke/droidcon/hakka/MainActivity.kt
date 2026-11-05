package ke.droidcon.hakka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ke.droidcon.hakka.otp.LocalOtpVerifier
import ke.droidcon.hakka.otp.OtpConfig
import ke.droidcon.hakka.otp.OtpViewModel
import ke.droidcon.hakka.ui.OtpScreen
import ke.droidcon.hakka.ui.theme.OtpTheme

class MainActivity : ComponentActivity() {

    private val viewModel: OtpViewModel by viewModels {
        OtpViewModel.Factory(LocalOtpVerifier(OtpConfig.VALID_CODE))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OtpTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                OtpScreen(
                    state = state,
                    onDigitChange = viewModel::onFieldChanged,
                    onBackspace = viewModel::onBackspace,
                    onFocusChange = viewModel::onFocusChanged,
                    onSubmit = viewModel::onVerify,
                    onResend = viewModel::onResend,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                )
            }
        }
    }
}
