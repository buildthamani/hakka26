package ke.droidcon.hakka

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp

/** The row of six digit boxes. Stateless: it reports every change through [onStateChange]. */
@Composable
internal fun OtpDigitRow(
    state: OtpState,
    colors: OtpColors,
    requesters: List<FocusRequester>,
    onStateChange: (OtpState) -> Unit,
) {
    Row(
        Modifier.padding(top = 24.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        repeat(OtpState.LENGTH) { i ->
            DigitBox(
                index = i, state = state, colors = colors, requester = requesters[i],
                onInput = { onStateChange(state.onInput(i, it)) },
                onBackspace = { onStateChange(state.onBackspace(i)) },
                onFocused = { if (state.focusedIndex != i) onStateChange(state.focus(i)) },
            )
        }
    }
}
