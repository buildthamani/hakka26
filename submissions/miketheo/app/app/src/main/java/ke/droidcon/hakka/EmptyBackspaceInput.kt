package ke.droidcon.hakka

import android.view.inputmethod.InputConnectionWrapper
import androidx.compose.runtime.*
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest

/**
 * An empty editor has no text to delete, so Compose's value callback cannot report
 * an IME backspace. Handle that Android command directly; all other editing stays
 * with the standard Compose input connection. No sentinel text enters the editor.
 */
@Composable
internal fun EmptyBackspaceInput(
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
    InterceptPlatformTextInput(interceptor, content)
}
