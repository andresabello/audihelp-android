package com.audihelp.app.data.dto.auth

import com.audihelp.app.domain.model.User
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthRedirectResponse(val url: String)

@JsonClass(generateAdapter = true)
data class AuthCallbackResponse(
    val token: String,
    val user: UserDto,
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String? = null,
)

fun UserDto.toDomain() = User(id = id, name = name, email = email, avatar = avatar)
