package ke.droidcon.hakka.theme

import androidx.compose.ui.graphics.Color

/** One theme's colours for the OTP sheet, read from `colours.json` (`otp.light` / `otp.dark`). */
data class OtpColors(
    val sheet: Color,
    val scrim: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val digitFilledBg: Color,
    val digitFilledText: Color,
    val digitFocusedBg: Color,
    val digitFocusedOutline: Color,
    val digitEmptyBg: Color,
    val digitEmptyOutline: Color,
    val digitPlaceholder: Color,
    val buttonBg: Color,
    val buttonText: Color,
    val buttonDisabledBg: Color,
    val link: Color,
    val success: Color,
    val error: Color,
) {
    companion object {
        fun from(values: Map<String, String>): OtpColors {
            fun c(key: String) = parseHexColor(values[key] ?: error("colours.json is missing \"$key\""))
            return OtpColors(
                sheet = c("sheet"),
                scrim = c("scrim"),
                textPrimary = c("text_primary"),
                textSecondary = c("text_secondary"),
                digitFilledBg = c("digit_filled_bg"),
                digitFilledText = c("digit_filled_text"),
                digitFocusedBg = c("digit_focused_bg"),
                digitFocusedOutline = c("digit_focused_outline"),
                digitEmptyBg = c("digit_empty_bg"),
                digitEmptyOutline = c("digit_empty_outline"),
                digitPlaceholder = c("digit_placeholder"),
                buttonBg = c("button_bg"),
                buttonText = c("button_text"),
                buttonDisabledBg = c("button_disabled_bg"),
                link = c("link"),
                success = c("success"),
                error = c("error"),
            )
        }
    }
}

data class OtpPalette(val light: OtpColors, val dark: OtpColors) {
    fun forTheme(dark: Boolean): OtpColors = if (dark) this.dark else light

    companion object {
        /** Parses the contents of `colours.json`. */
        fun parse(json: String): OtpPalette {
            val root = JsonObjectReader(json).readRoot()
            val otp = root.obj("otp")
            return OtpPalette(
                light = OtpColors.from(otp.obj("light").strings()),
                dark = OtpColors.from(otp.obj("dark").strings()),
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun Map<String, Any>.obj(key: String): Map<String, Any> =
            this[key] as? Map<String, Any> ?: error("colours.json is missing object \"$key\"")

        private fun Map<String, Any>.strings(): Map<String, String> =
            mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap()
    }
}

/** Parses `#RRGGBB` or `#AARRGGBB`. */
fun parseHexColor(hex: String): Color {
    val digits = hex.trim().removePrefix("#")
    val value = digits.toLong(16)
    return when (digits.length) {
        6 -> Color(0xFF000000 or value)
        8 -> Color(value)
        else -> throw IllegalArgumentException("Unsupported colour \"$hex\"")
    }
}

/**
 * Minimal reader for the object-and-string JSON in `colours.json`,
 * so the app needs no extra serialization dependency.
 */
private class JsonObjectReader(private val text: String) {
    private var pos = 0

    fun readRoot(): Map<String, Any> = readObject().also {
        skipWhitespace()
        require(pos == text.length) { "Unexpected trailing content at $pos" }
    }

    private fun readValue(): Any {
        skipWhitespace()
        return when (peek()) {
            '{' -> readObject()
            '"' -> readString()
            else -> readLiteral()
        }
    }

    private fun readObject(): Map<String, Any> {
        expect('{')
        val result = LinkedHashMap<String, Any>()
        skipWhitespace()
        if (peek() == '}') { pos++; return result }
        while (true) {
            skipWhitespace()
            val key = readString()
            skipWhitespace()
            expect(':')
            result[key] = readValue()
            skipWhitespace()
            when (text.getOrNull(pos++)) {
                ',' -> continue
                '}' -> return result
                else -> throw IllegalArgumentException("Expected ',' or '}' at ${pos - 1}")
            }
        }
    }

    private fun readString(): String {
        expect('"')
        val sb = StringBuilder()
        while (true) {
            val c = text.getOrNull(pos++) ?: throw IllegalArgumentException("Unterminated string")
            when (c) {
                '"' -> return sb.toString()
                '\\' -> sb.append(text.getOrNull(pos++) ?: throw IllegalArgumentException("Bad escape"))
                else -> sb.append(c)
            }
        }
    }

    /** Numbers, booleans and null are kept as raw text; the palette never uses them. */
    private fun readLiteral(): String {
        val start = pos
        while (pos < text.length && text[pos] !in ",}] \n\r\t") pos++
        require(pos > start) { "Expected a value at $start" }
        return text.substring(start, pos)
    }

    private fun peek(): Char? = text.getOrNull(pos)

    private fun expect(c: Char) {
        skipWhitespace()
        require(text.getOrNull(pos) == c) { "Expected '$c' at $pos" }
        pos++
    }

    private fun skipWhitespace() {
        while (pos < text.length && text[pos].isWhitespace()) pos++
    }
}
