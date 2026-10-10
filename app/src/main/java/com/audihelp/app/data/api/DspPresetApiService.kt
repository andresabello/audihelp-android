package com.audihelp.app.data.api

import com.audihelp.app.data.dto.ApiItemResponse
import com.audihelp.app.data.dto.ApiListResponse
import com.audihelp.app.data.dto.dsp.DspPresetDto
import com.audihelp.app.data.dto.dsp.SavePresetRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface DspPresetApiService {
    @GET("v1/dsp-presets")
    suspend fun getPresets(): ApiListResponse<DspPresetDto>

    @POST("v1/dsp-presets")
    suspend fun createPreset(@Body request: SavePresetRequest): ApiItemResponse<DspPresetDto>

    @GET("v1/dsp-presets/{id}")
    suspend fun getPreset(@Path("id") id: String): ApiItemResponse<DspPresetDto>

    @PUT("v1/dsp-presets/{id}")
    suspend fun updatePreset(@Path("id") id: String, @Body request: SavePresetRequest): ApiItemResponse<DspPresetDto>

    @DELETE("v1/dsp-presets/{id}")
    suspend fun deletePreset(@Path("id") id: String)

    @POST("v1/dsp-presets/{id}/activate")
    suspend fun activatePreset(@Path("id") id: String): ApiItemResponse<DspPresetDto>
}
