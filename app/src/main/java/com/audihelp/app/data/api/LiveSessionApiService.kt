package com.audihelp.app.data.api

import com.audihelp.app.data.dto.ApiItemResponse
import com.audihelp.app.data.dto.ApiListResponse
import com.audihelp.app.data.dto.livesession.CreateLiveSessionRequest
import com.audihelp.app.data.dto.livesession.LiveSessionDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface LiveSessionApiService {
    @GET("v1/live-sessions")
    suspend fun getSessions(): ApiListResponse<LiveSessionDto>

    @POST("v1/live-sessions")
    suspend fun startSession(@Body request: CreateLiveSessionRequest): ApiItemResponse<LiveSessionDto>

    @POST("v1/live-sessions/{id}/end")
    suspend fun endSession(@Path("id") id: String): ApiItemResponse<LiveSessionDto>

    @DELETE("v1/live-sessions/{id}")
    suspend fun deleteSession(@Path("id") id: String)
}
