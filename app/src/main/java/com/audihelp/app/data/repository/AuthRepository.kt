package com.audihelp.app.data.repository

import com.audihelp.app.data.local.AppDataStore
import com.audihelp.app.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val dataStore: AppDataStore,
) {
    fun isLoggedIn(): Flow<Boolean> = flow { emit(true) }

    suspend fun fetchDevToken(): Result<Unit> = Result.success(Unit)

    suspend fun getGoogleRedirectUrl(): Result<String> = Result.success("")

    suspend fun saveTokenAndFetchUser(token: String): Result<User> = Result.success(
        User(id = "local", name = "Local User", email = "local@device", avatar = null)
    )

    suspend fun logout() {
        dataStore.clearAll()
    }
}
