package uz.rento.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST
import uz.rento.data.remote.dto.ApiResponse
import uz.rento.data.remote.dto.RefreshTokenRequest
import uz.rento.data.remote.dto.RefreshTokenResponse
import uz.rento.data.remote.dto.SendOtpRequest
import uz.rento.data.remote.dto.SendOtpResponse
import uz.rento.data.remote.dto.VerifyOtpRequest
import uz.rento.data.remote.dto.VerifyOtpResponse

/**
 * AuthApi — Auth service Retrofit interfeysi
 * Base URL: http://10.0.2.2:3001/api/v1/ (dev)
 */
interface AuthApi {

    @POST("auth/send-otp")
    suspend fun sendOtp(
        @Body request: SendOtpRequest
    ): ApiResponse<SendOtpResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): ApiResponse<VerifyOtpResponse>

    @POST("auth/refresh-token")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): ApiResponse<RefreshTokenResponse>

    @POST("auth/logout")
    suspend fun logout(): ApiResponse<Unit>
}
