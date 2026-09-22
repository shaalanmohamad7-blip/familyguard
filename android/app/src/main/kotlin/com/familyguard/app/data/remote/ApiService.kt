package com.familyguard.app.data.remote

import com.familyguard.app.data.remote.dto.FamilyInfoResponse
import com.familyguard.app.data.remote.dto.FcmTokenRequest
import com.familyguard.app.data.remote.dto.GenericSuccessResponse
import com.familyguard.app.data.remote.dto.GeofenceEventRequest
import com.familyguard.app.data.remote.dto.GeofencesResponse
import com.familyguard.app.data.remote.dto.LocationReportRequest
import com.familyguard.app.data.remote.dto.PairingRedeemRequest
import com.familyguard.app.data.remote.dto.PairingRedeemResponse
import com.familyguard.app.data.remote.dto.SosRequest
import com.familyguard.app.data.remote.dto.StatusRequest
import com.familyguard.app.data.remote.dto.UnpairResponse
import com.familyguard.app.data.remote.dto.UsageReportRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * The FamilyGuard REST backend. This app never talks to Firestore or any other database
 * directly — every read and write goes through these Next.js API routes over HTTPS, and
 * every call (via [AuthInterceptor]) carries the device's bearer token.
 */
interface ApiService {

    @POST("api/pairing/redeem")
    suspend fun redeemPairingCode(@Body request: PairingRedeemRequest): Response<PairingRedeemResponse>

    @GET("api/devices/{deviceId}/family-info")
    suspend fun getFamilyInfo(@Path("deviceId") deviceId: String): Response<FamilyInfoResponse>

    @POST("api/devices/{deviceId}/sos")
    suspend fun sendSos(
        @Path("deviceId") deviceId: String,
        @Body request: SosRequest
    ): Response<GenericSuccessResponse>

    @POST("api/devices/{deviceId}/location")
    suspend fun reportLocation(
        @Path("deviceId") deviceId: String,
        @Body request: LocationReportRequest
    ): Response<GenericSuccessResponse>

    @POST("api/devices/{deviceId}/status")
    suspend fun reportStatus(
        @Path("deviceId") deviceId: String,
        @Body request: StatusRequest
    ): Response<GenericSuccessResponse>

    @GET("api/devices/{deviceId}/geofences")
    suspend fun getGeofences(@Path("deviceId") deviceId: String): Response<GeofencesResponse>

    @POST("api/devices/{deviceId}/geofence-events")
    suspend fun reportGeofenceEvent(
        @Path("deviceId") deviceId: String,
        @Body request: GeofenceEventRequest
    ): Response<GenericSuccessResponse>

    @POST("api/devices/{deviceId}/usage")
    suspend fun reportUsage(
        @Path("deviceId") deviceId: String,
        @Body request: UsageReportRequest
    ): Response<GenericSuccessResponse>

    @POST("api/devices/{deviceId}/fcm-token")
    suspend fun registerFcmToken(
        @Path("deviceId") deviceId: String,
        @Body request: FcmTokenRequest
    ): Response<GenericSuccessResponse>

    @POST("api/devices/{deviceId}/unpair")
    suspend fun unpairDevice(@Path("deviceId") deviceId: String): Response<UnpairResponse>
}
