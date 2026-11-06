package ke.droidcon.hakka.domain.usecase

import ke.droidcon.hakka.domain.model.VALID_CODE

class VerifyOtpUseCase(private val validCode: String = VALID_CODE) {
    operator fun invoke(code: String): Boolean = code == validCode
}
