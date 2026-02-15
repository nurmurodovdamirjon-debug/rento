package uz.rento.data.remote.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.QueryMap
import uz.rento.data.remote.dto.ApiResponse
import uz.rento.data.remote.dto.ListingDto
import uz.rento.data.remote.dto.ListingListDto
import uz.rento.data.remote.dto.NearbyListingListDto
import uz.rento.data.remote.dto.CreateListingRequest
import uz.rento.data.remote.dto.UpdateListingRequest
import uz.rento.data.remote.dto.UpdateStatusRequest
import uz.rento.data.remote.dto.PaginatedResponse
import uz.rento.data.remote.dto.StatsDto

/**
 * ListingApi — E'lonlar CRUD endpointlari
 * Base URL: http://10.0.2.2:3002/api/v1/ (dev)
 */
interface ListingApi {

    @GET("listings")
    suspend fun getListings(
        @QueryMap filters: Map<String, String>
    ): ApiResponse<PaginatedResponse<ListingListDto>>

    @GET("listings/{id}")
    suspend fun getListing(
        @Path("id") id: String
    ): ApiResponse<ListingDto>

    @POST("listings")
    suspend fun createListing(
        @Body request: CreateListingRequest
    ): ApiResponse<ListingDto>

    @PUT("listings/{id}")
    suspend fun updateListing(
        @Path("id") id: String,
        @Body request: UpdateListingRequest
    ): ApiResponse<ListingDto>

    @DELETE("listings/{id}")
    suspend fun deleteListing(
        @Path("id") id: String
    ): retrofit2.Response<Unit>

    @PUT("listings/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: String,
        @Body request: UpdateStatusRequest
    ): ApiResponse<Any>

    @GET("listings/{id}/stats")
    suspend fun getStats(
        @Path("id") id: String
    ): ApiResponse<StatsDto>

    @GET("listings/my")
    suspend fun getMyListings(
        @QueryMap filters: Map<String, String>
    ): ApiResponse<PaginatedResponse<ListingListDto>>

    @GET("listings/search")
    suspend fun searchListings(
        @QueryMap filters: Map<String, String>
    ): ApiResponse<PaginatedResponse<ListingListDto>>

    @GET("listings/nearby")
    suspend fun getNearbyListings(
        @QueryMap filters: Map<String, String>
    ): ApiResponse<PaginatedResponse<NearbyListingListDto>>
}
