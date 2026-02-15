package uz.rento.data.remote.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import uz.rento.data.remote.dto.ApiResponse
import uz.rento.data.remote.dto.NotificationListDto
import uz.rento.data.remote.dto.RegisterFcmTokenRequest
import uz.rento.data.remote.dto.UnreadCountResponse
import uz.rento.data.remote.dto.MarkAllReadResponse

/**
 * NotificationApi — Bildirishnomalar endpointlari
 * Base URL: http://10.0.2.2:3002/api/v1/ (dev)
 */
interface NotificationApi {

    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): ApiResponse<NotificationListDto>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): ApiResponse<UnreadCountResponse>

    @PUT("notifications/{id}/read")
    suspend fun markAsRead(
        @Path("id") id: String
    ): retrofit2.Response<Unit>

    @PUT("notifications/read-all")
    suspend fun markAllAsRead(): ApiResponse<MarkAllReadResponse>

    @POST("notifications/fcm-token")
    suspend fun registerFcmToken(
        @Body request: RegisterFcmTokenRequest
    ): ApiResponse<Any>

    @DELETE("notifications/fcm-token")
    suspend fun unregisterFcmToken(
        @Body request: Map<String, String>
    ): retrofit2.Response<Unit>
}
