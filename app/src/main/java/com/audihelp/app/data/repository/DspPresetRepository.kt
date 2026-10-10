package com.audihelp.app.data.repository

import com.audihelp.app.data.local.AppDataStore
import com.audihelp.app.data.local.db.DspPresetDao
import com.audihelp.app.data.local.db.DspPresetEntity
import com.audihelp.app.data.local.db.toDomain
import com.audihelp.app.data.local.db.toJson
import com.audihelp.app.domain.model.DspConfig
import com.audihelp.app.domain.model.DspPreset
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DspPresetRepository @Inject constructor(
    private val dao: DspPresetDao,
    private val dataStore: AppDataStore,
) {

    suspend fun getPresets(): Result<List<DspPreset>> = runCatching {
        dao.getAll().map { it.toDomain() }
    }

    suspend fun getPresetsByProfile(profileId: String): Result<List<DspPreset>> = runCatching {
        dao.getByProfileId(profileId).map { it.toDomain() }
    }

    suspend fun createPreset(name: String, description: String?, config: DspConfig, profileId: String? = null): Result<DspPreset> = runCatching {
        val now = Instant.now().toString()
        val entity = DspPresetEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            configJson = config.toJson(),
            isActive = false,
            profileId = profileId,
            createdAt = now,
        )
        dao.insert(entity)
        entity.toDomain()
    }

    suspend fun updatePreset(id: String, name: String, description: String?, config: DspConfig): Result<DspPreset> = runCatching {
        val existing = dao.getById(id) ?: error("Preset not found: $id")
        val updated = existing.copy(
            name = name,
            description = description,
            configJson = config.toJson(),
        )
        dao.update(updated)
        updated.toDomain()
    }

    suspend fun activatePreset(id: String): Result<DspPreset> = runCatching {
        dao.deactivateAll()
        dao.activate(id)
        dataStore.saveActivePresetId(id)
        val entity = dao.getById(id) ?: error("Preset not found: $id")
        entity.toDomain()
    }

    suspend fun deletePreset(id: String): Result<Unit> = runCatching {
        dao.delete(id)
    }
}
