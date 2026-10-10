package com.audihelp.app.data.dto

import com.squareup.moshi.JsonClass

/** Wraps Laravel ResourceCollection responses: { "data": [...] } */
@JsonClass(generateAdapter = true)
data class ApiListResponse<T>(val data: List<T>)

/** Wraps Laravel JsonResource responses: { "data": {...} } */
@JsonClass(generateAdapter = true)
data class ApiItemResponse<T>(val data: T)
