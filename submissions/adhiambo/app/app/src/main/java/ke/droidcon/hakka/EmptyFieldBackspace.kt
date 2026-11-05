package ke.droidcon.hakka

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest

/**
 * Empty fields have no text change to report when an IME requests backward deletion.
 * Observe that request directly, leaving normal editing and hardware keys to Compose.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun EmptyFieldBackspace(
    enabled: Boolean,
    onBackspace: () -> Unit,
    content: @Composable () -> Unit,
) {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentBackspace by rememberUpdatedState(onBackspace)
    val interceptor = remember {
        PlatformTextInputInterceptor { request, next ->
            next.startInputMethod(object : PlatformTextInputMethodRequest {
                override fun createInputConnection(outAttributes: EditorInfo): InputConnection =
                    EmptyFieldInputConnection(request.createInputConnection(outAttributes)) {
                        if (currentEnabled) {
                            currentBackspace()
                            true
                        } else false
                    }
            })
        }
    }
    InterceptPlatformTextInput(interceptor, content)
}

internal class EmptyFieldInputConnection(
    target: InputConnection,
    private val onEmptyBackspace: () -> Boolean,
) : InputConnectionWrapper(target, false) {
    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean =
        if (beforeLength > 0 && afterLength == 0 && onEmptyBackspace()) true
        else super.deleteSurroundingText(beforeLength, afterLength)

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean =
        if (beforeLength > 0 && afterLength == 0 && onEmptyBackspace()) true
        else super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
}
