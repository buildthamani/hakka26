package ke.droidcon.hakka.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ke.droidcon.hakka.R
import ke.droidcon.hakka.presentation.DarkOtpColors
import ke.droidcon.hakka.presentation.LightOtpColors
import ke.droidcon.hakka.presentation.OtpColors
import ke.droidcon.hakka.presentation.OtpViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OtpScreen(
    viewModel: OtpViewModel = remember { OtpViewModel() },
    modifier: Modifier = Modifier,
) {
    val colors = if (isSystemInDarkTheme()) DarkOtpColors else LightOtpColors
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        // Dimmed / blurred background behind bottom sheet
        BehindSheet(colors = colors)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.scrim),
        )

        // Stateless OTP bottom sheet content
        OtpBottomSheetContent(
            state = state,
            colors = colors,
            onDigitInput = viewModel::onDigitInput,
            onBackspace = viewModel::onBackspace,
            onFocused = viewModel::onFocusChanged,
            onPaste = viewModel::onPaste,
            onSubmit = viewModel::onSubmit,
            onResend = viewModel::onResend,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun BehindSheet(colors: OtpColors) {
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier.blur(12.dp)
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.sheet)
            .then(blurModifier)
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.otp_welcome_back),
            color = colors.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
