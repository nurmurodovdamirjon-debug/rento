package uz.rento.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import uz.rento.data.remote.dto.ApiResponse
import uz.rento.data.remote.dto.ChatRoomDetailDto
import uz.rento.data.remote.dto.ChatRoomListDto
import uz.rento.data.remote.dto.CreateChatRequest
import uz.rento.data.remote.dto.MessageDto
import uz.rento.data.remote.dto.MessageListDto
import uz.rento.data.remote.dto.SendMessageRequest

/**
 * ChatApi — Chat xizmati REST endpointlari
 * Base URL: http://10.0.2.2:3003/api/v1/ (dev)
 */
interface ChatApi {

    /** Yangi chat boshlash yoki mavjud chatni olish */
    @POST("chats")
    suspend fun createChat(
        @Body request: CreateChatRequest
    ): ApiResponse<ChatRoomDetailDto>

    /** Chat xonalari ro'yxati */
    @GET("chats")
    suspend fun getChats(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): ApiResponse<ChatRoomListDto>

    /** Xabarlar tarixi */
    @GET("chats/{room_id}/messages")
    suspend fun getMessages(
        @Path("room_id") roomId: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 50
    ): ApiResponse<MessageListDto>

    /** Xabar yuborish (REST fallback) */
    @POST("chats/{room_id}/messages")
    suspend fun sendMessage(
        @Path("room_id") roomId: String,
        @Body request: SendMessageRequest
    ): ApiResponse<MessageDto>

    /** O'qildi belgilash */
    @PUT("chats/{room_id}/read")
    suspend fun markAsRead(
        @Path("room_id") roomId: String
    ): Response<Unit>
}
