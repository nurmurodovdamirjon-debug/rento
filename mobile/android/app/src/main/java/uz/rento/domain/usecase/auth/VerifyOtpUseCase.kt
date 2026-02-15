package uz.rento.domain.usecase.auth

import uz.rento.domain.model.User
import uz.rento.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * VerifyOtpUseCase — OTP tasdiqlash va tizimga kirish
 */
class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phone: String, otp: String): Result<User> {
        // OTP validatsiyasi
        if (!otp.matches(Regex("^\\d{6}$"))) {
            return Result.failure(
                IllegalArgumentException("OTP 6 raqamdan iborat bo'lishi kerak")
            )
        }
        return authRepository.verifyOtp(phone, otp)
    }
}
