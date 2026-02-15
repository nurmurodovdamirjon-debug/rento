package uz.rento.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import uz.rento.data.remote.dto.ApiResponse
import uz.rento.data.remote.dto.FavoriteIdsResponse
import uz.rento.data.remote.dto.FavoriteListDto
import uz.rento.data.remote.dto.FavoriteCheckResponse
import uz.rento.data.remote.dto.ToggleFavoriteResponse

/**
 * FavoriteApi — Sevimlilar endpointlari
 * Base URL: http://10.0.2.2:3002/api/v1/ (dev)
 */
interface FavoriteApi {

    @POST("favorites/{listing_id}")
    suspend fun toggle(
        @Path("listing_id") listingId: String
    ): ApiResponse<ToggleFavoriteResponse>

    @GET("favorites")
    suspend fun getFavorites(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): ApiResponse<FavoriteListDto>

    @GET("favorites/check/{listing_id}")
    suspend fun check(
        @Path("listing_id") listingId: String
    ): ApiResponse<FavoriteCheckResponse>

    @GET("favorites/ids")
    suspend fun getFavoriteIds(): ApiResponse<FavoriteIdsResponse>
}
