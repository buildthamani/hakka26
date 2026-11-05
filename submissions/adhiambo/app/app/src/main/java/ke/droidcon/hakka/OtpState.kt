package ke.droidcon.hakka

import java.io.Serializable

/** Pure OTP transitions, shared by the Compose screen and the CI unit tests. */
data class OtpState(
    val digits: List<String> = List(6) { "" },
    val active: Int = 0,
    val status: String = "",
    val deadlineMillis: Long,
    val focusRequest: Int = 0,
) : Serializable {
    val complete: Boolean get() = digits.all { it.length == 1 }
    val successful: Boolean get() = status == SUCCESS

    fun input(index: Int, text: String): OtpState {
        val numeric = text.filter { it in '0'..'9' }.take(6 - index)
        if (numeric.isEmpty()) return this
        val updated = digits.toMutableList()
        numeric.forEachIndexed { offset, digit -> updated[index + offset] = digit.toString() }
        return copy(digits = updated, active = (index + numeric.length).coerceAtMost(5), status = "")
    }

    fun delete(index: Int): OtpState {
        val target = if (digits[index].isEmpty()) (index - 1).coerceAtLeast(0) else index
        return copy(
            digits = digits.toMutableList().also { it[target] = "" },
            active = target,
            status = "",
            focusRequest = focusRequest + 1,
        )
    }

    fun submit(): OtpState = when {
        !complete -> this
        digits.joinToString("") == "123456" -> copy(status = SUCCESS)
        else -> copy(digits = List(6) { "" }, active = 0, status = ERROR,
            focusRequest = focusRequest + 1)
    }

    fun secondsRemaining(nowMillis: Long): Int =
        ((deadlineMillis - nowMillis).coerceAtLeast(0) + 999).div(1000).toInt()

    fun resend(nowMillis: Long): OtpState = if (secondsRemaining(nowMillis) > 0) this
        else copy(deadlineMillis = nowMillis + 30_000, status = "")

    companion object {
        const val SUCCESS = "You're in."
        const val ERROR = "That code didn't work. Try again."
        fun initial(nowMillis: Long) = OtpState(deadlineMillis = nowMillis + 30_000)
    }
}
