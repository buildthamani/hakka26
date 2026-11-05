package ke.droidcon.hakka

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.TextKeyListener
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.core.view.SoftwareKeyboardControllerCompat
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView

/** Receives both hardware delete and IME deletion on an empty field (TextWatcher cannot). */
internal class DigitEditText(context: Context) : EditText(context) {
    var onDelete: () -> Unit = {}
    var onInput: (String) -> Unit = {}
    var onFocused: () -> Unit = {}
    var onSubmit: () -> Unit = {}
    private var rendering = false

    init {
        background = null
        setPadding(0, 0, 0, 0)
        gravity = Gravity.CENTER
        textSize = 18f
        setSingleLine(true)
        // Keep paste text intact; the pure input policy extracts ASCII digits itself.
        keyListener = TextKeyListener.getInstance()
        setRawInputType(InputType.TYPE_CLASS_NUMBER)
        isCursorVisible = false
        setSelectAllOnFocus(true)
        importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO
        setOnFocusChangeListener { _, focused -> if (focused) { selectAll(); onFocused() } }
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!rendering) onInput(s.toString())
            }
        })
        setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) { onSubmit(); true } else false
        }
    }

    fun render(value: String) {
        if (text.toString() == value) return
        rendering = true
        setText(value)
        setSelection(value.length)
        rendering = false
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) onInput(clip.getItemAt(0).coerceToText(context).toString())
            return true
        }
        return super.onTextContextMenuItem(id)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val original = super.onCreateInputConnection(outAttrs) ?: return null
        return object : InputConnectionWrapper(original, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                onInput(text?.toString().orEmpty())
                return true
            }
            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (beforeLength > 0 && afterLength == 0) { onDelete(); return true }
                return super.deleteSurroundingText(beforeLength, afterLength)
            }
            override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
                if (beforeLength > 0 && afterLength == 0) { onDelete(); return true }
                return super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL) { onDelete(); return true }
        val character = event.unicodeChar.toChar()
        if (character in '0'..'9') { onInput(character.toString()); return true }
        if (event.unicodeChar != 0) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        if (keyCode == KeyEvent.KEYCODE_DEL) true else super.onKeyUp(keyCode, event)
}

@Composable
internal fun OtpDigitField(
    index: Int,
    value: String,
    focused: Boolean,
    focusRequest: Int,
    colors: OtpColors,
    size: Dp,
    editors: MutableMap<Int, DigitEditText>,
    onInput: (String) -> Int,
    onDelete: () -> Int,
    onFocused: () -> Unit,
    onSubmit: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val editor = remember(context) { DigitEditText(context) }
    DisposableEffect(editor) {
        editors[index] = editor
        onDispose { editors.remove(index) }
    }
    val fill = when { focused -> colors["digit_focused_bg"]; value.isNotEmpty() -> colors["digit_filled_bg"]; else -> colors["digit_empty_bg"] }
    val foreground = if (value.isNotEmpty() && !focused) colors["digit_filled_text"] else colors["text_primary"]
    val outline = if (focused) colors["digit_focused_outline"] else colors["digit_empty_outline"]
    LaunchedEffect(focused, focusRequest) {
        if (focused) {
            editor.requestFocus()
            editor.post {
                if (editor.hasFocus()) {
                    SoftwareKeyboardControllerCompat(editor).show()
                }
            }
        }
    }
    Box(Modifier.size(size).testTag("otp_digit_${index + 1}")
        .background(fill, CircleShape)
        .then(if (focused || value.isEmpty()) Modifier.border(1.dp, outline, CircleShape) else Modifier)) {
        AndroidView(factory = { editor }, modifier = Modifier.size(size), update = { view ->
            view.contentDescription = "Digit ${index + 1} of 6"
            view.tag = "otp_digit_${index + 1}"
            view.hint = "–"
            view.setHintTextColor(colors["digit_placeholder"].toArgb())
            view.setTextColor(foreground.toArgb())
            view.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            // Move native focus synchronously: another hardware key may arrive before recomposition.
            view.onInput = { raw ->
                val next = onInput(raw)
                editors[next]?.requestFocus()
                view.render(value)
            }
            view.onDelete = {
                val next = onDelete()
                editors[next]?.requestFocus()
            }
            view.onFocused = onFocused
            view.onSubmit = onSubmit
            view.render(value)
        })
    }
}
