package com.audihelp.app.data.api

import com.audihelp.app.data.dto.ApiItemResponse
import com.audihelp.app.data.dto.ApiListResponse
import com.audihelp.app.data.dto.profile.AddThresholdRequest
import com.audihelp.app.data.dto.profile.CreateProfileRequest
import com.audihelp.app.data.dto.profile.HearingProfileDto
import com.audihelp.app.data.dto.profile.HearingThresholdDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProfileApiService {
    @GET("v1/profiles")
    suspend fun getProfiles(): ApiListResponse<HearingProfileDto>

    @POST("v1/profiles")
    suspend fun createProfile(@Body request: CreateProfileRequest): ApiItemResponse<HearingProfileDto>

    @GET("v1/profiles/{id}")
    suspend fun getProfile(@Path("id") id: String): ApiItemResponse<HearingProfileDto>

    @PUT("v1/profiles/{id}")
    suspend fun updateProfile(@Path("id") id: String, @Body request: CreateProfileRequest): ApiItemResponse<HearingProfileDto>

    @DELETE("v1/profiles/{id}")
    suspend fun deleteProfile(@Path("id") id: String)

    @POST("v1/profiles/{id}/activate")
    suspend fun activateProfile(@Path("id") id: String): ApiItemResponse<HearingProfileDto>

    @POST("v1/profiles/{id}/thresholds")
    suspend fun addThreshold(
        @Path("id") profileId: String,
        @Body request: AddThresholdRequest,
    ): ApiItemResponse<HearingThresholdDto>
}
