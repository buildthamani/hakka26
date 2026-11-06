package ke.droidcon.hakka

import android.view.inputmethod.InputConnectionWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest

/**
 * Routes physical and software keyboard backspace to the same callback.
 * Only empty-editor IME deletion needs an adapter: it cannot change the field's
 * value. Ordinary text edits stay with Compose's standard input connection.
 */
@Composable
internal fun OtpKeyboardInput(
    empty: Boolean,
    onBackspace: () -> Unit,
    content: @Composable () -> Unit,
) {
    val isEmpty by rememberUpdatedState(empty)
    val backspace by rememberUpdatedState(onBackspace)
    val interceptor = remember {
        PlatformTextInputInterceptor { request, next ->
            next.startInputMethod(PlatformTextInputMethodRequest { attributes ->
                object : InputConnectionWrapper(request.createInputConnection(attributes), false) {
                    private fun deleteEmpty(before: Int, after: Int): Boolean {
                        if (!isEmpty || before <= 0 || after != 0) return false
                        backspace()
                        return true
                    }
                    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean =
                        deleteEmpty(beforeLength, afterLength) ||
                            super.deleteSurroundingText(beforeLength, afterLength)

                    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean =
                        deleteEmpty(beforeLength, afterLength) ||
                            super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
                }
            })
        }
    }
    InterceptPlatformTextInput(interceptor) {
        Box(
            Modifier.onPreviewKeyEvent { event ->
                if (event.key != Key.Backspace) {
                    false
                } else {
                    if (event.type == KeyEventType.KeyDown) backspace()
                    true
                }
            },
        ) { content() }
    }
}
