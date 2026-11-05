package ke.droidcon.hakka

fun checkCode(code: String): String =
    if (code == "123456") "You're in." else "That code didn't work. Try again."
