package com.audihelp.app.data.api

import com.audihelp.app.data.dto.ApiItemResponse
import com.audihelp.app.data.dto.ApiListResponse
import com.audihelp.app.data.dto.hearingtest.HearingTestSessionDto
import com.audihelp.app.data.dto.hearingtest.HearingTestTrialDto
import com.audihelp.app.data.dto.hearingtest.RecordTrialRequest
import com.audihelp.app.data.dto.hearingtest.StartSessionRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface HearingTestApiService {
    @GET("v1/hearing-tests")
    suspend fun getSessions(): ApiListResponse<HearingTestSessionDto>

    @POST("v1/hearing-tests")
    suspend fun startSession(@Body request: StartSessionRequest): ApiItemResponse<HearingTestSessionDto>

    @GET("v1/hearing-tests/{id}")
    suspend fun getSession(@Path("id") id: String): ApiItemResponse<HearingTestSessionDto>

    @POST("v1/hearing-tests/{id}/trials")
    suspend fun recordTrial(
        @Path("id") sessionId: String,
        @Body request: RecordTrialRequest,
    ): ApiItemResponse<HearingTestTrialDto>

    @POST("v1/hearing-tests/{id}/complete")
    suspend fun completeSession(@Path("id") id: String): ApiItemResponse<HearingTestSessionDto>

    @DELETE("v1/hearing-tests/{id}")
    suspend fun deleteSession(@Path("id") id: String)
}
