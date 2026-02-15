package uz.rento.domain.usecase.auth

import uz.rento.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * SendOtpUseCase — SMS OTP yuborish
 */
class SendOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phone: String): Result<Unit> {
        // Telefon raqam validatsiyasi
        if (!phone.matches(Regex("^\\+998\\d{9}$"))) {
            return Result.failure(
                IllegalArgumentException("Noto'g'ri telefon raqam formati")
            )
        }
        return authRepository.sendOtp(phone)
    }
}
