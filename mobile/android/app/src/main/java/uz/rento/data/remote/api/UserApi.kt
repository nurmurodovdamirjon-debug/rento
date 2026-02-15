package uz.rento.data.remote.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import uz.rento.data.remote.dto.ApiResponse
import uz.rento.data.remote.dto.PublicUserDto
import uz.rento.data.remote.dto.UpdateProfileRequest
import uz.rento.data.remote.dto.UserDto

/**
 * UserApi — Core API User endpointlari
 * Base URL: http://10.0.2.2:3002/api/v1/ (dev)
 */
interface UserApi {

    @GET("users/me")
    suspend fun getMyProfile(): ApiResponse<UserDto>

    @PUT("users/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): ApiResponse<UserDto>

    @GET("users/{id}")
    suspend fun getPublicProfile(
        @Path("id") userId: String
    ): ApiResponse<PublicUserDto>
}
