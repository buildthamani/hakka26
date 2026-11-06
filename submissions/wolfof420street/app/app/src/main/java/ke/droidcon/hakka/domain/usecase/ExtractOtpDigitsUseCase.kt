package ke.droidcon.hakka.domain.usecase

import ke.droidcon.hakka.domain.model.OTP_LENGTH

class ExtractOtpDigitsUseCase(private val maxLength: Int = OTP_LENGTH) {
    operator fun invoke(text: String): String =
        text.filter(Char::isDigit).take(maxLength)
}
