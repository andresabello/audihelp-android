package com.audihelp.app.data.api

import com.audihelp.app.data.dto.ApiItemResponse
import com.audihelp.app.data.dto.auth.AuthCallbackResponse
import com.audihelp.app.data.dto.auth.AuthRedirectResponse
import com.audihelp.app.data.dto.auth.UserDto
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("v1/auth/dev-token")
    suspend fun getDevToken(): AuthCallbackResponse

    @GET("v1/auth/google")
    suspend fun getGoogleRedirectUrl(): AuthRedirectResponse

    @GET("v1/auth/me")
    suspend fun getMe(): ApiItemResponse<UserDto>

    @POST("v1/auth/logout")
    suspend fun logout()
}
