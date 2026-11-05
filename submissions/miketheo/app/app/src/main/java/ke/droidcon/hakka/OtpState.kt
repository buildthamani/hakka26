package ke.droidcon.hakka

data class OtpState(
    val digits: List<String> = List(6) { "" },
    val focus: Int = 0,
    val status: String = "",
) {
    val complete get() = digits.all { it.length == 1 }

    fun input(index: Int, raw: String): OtpState {
        val clean = raw.filter { it in '0'..'9' }
        if (clean.isEmpty()) return this
        val start = if (clean.length >= 6) 0 else index
        val next = digits.toMutableList()
        clean.take(6 - start).forEachIndexed { offset, c -> next[start + offset] = c.toString() }
        return copy(digits = next, focus = (start + clean.length).coerceAtMost(5), status = "")
    }

    fun backspace(index: Int): OtpState {
        val target = if (digits[index].isEmpty()) (index - 1).coerceAtLeast(0) else index
        return copy(digits = digits.toMutableList().also { it[target] = "" }, focus = target, status = "")
    }

    fun submit(): OtpState = when {
        !complete -> this
        digits.joinToString("") == "123456" -> copy(status = "You're in.")
        else -> OtpState(status = "That code didn't work. Try again.")
    }
}

fun remainingSeconds(deadline: Long, now: Long): Int =
    ((deadline - now).coerceAtLeast(0) / 1000.0).let { kotlin.math.ceil(it).toInt() }
