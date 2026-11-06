package ke.droidcon.hakka.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import ke.droidcon.hakka.domain.model.OTP_LENGTH
import ke.droidcon.hakka.domain.model.OtpState
import ke.droidcon.hakka.presentation.OtpColors

@Composable
fun OtpInputRow(
    state: OtpState,
    colors: OtpColors,
    onInput: (index: Int, text: String) -> Unit,
    onBackspace: (index: Int) -> Unit,
    onFocused: (index: Int) -> Unit,
    onPaste: (text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequesters = remember { List(OTP_LENGTH) { FocusRequester() } }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.focusedIndex, state.status) {
        if (state.focusedIndex in focusRequesters.indices) {
            focusRequesters[state.focusedIndex].requestFocus()
            keyboardController?.show()
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(OTP_LENGTH) { index ->
            OtpDigitBox(
                index = index,
                digit = state.digits[index],
                focused = state.focusedIndex == index,
                colors = colors,
                focusRequester = focusRequesters[index],
                onInput = { onInput(index, it) },
                onBackspace = { onBackspace(index) },
                onFocused = { onFocused(index) },
                onPaste = onPaste,
            )
        }
    }
}
