package com.audihelp.app.data.local.db

import androidx.room.*

@Dao
interface HearingTestDao {
    @Query("SELECT * FROM hearing_test_sessions ORDER BY createdAt DESC")
    suspend fun getAllSessions(): List<HearingTestSessionEntity>

    @Query("SELECT * FROM hearing_test_sessions WHERE id = :id")
    suspend fun getSession(id: String): HearingTestSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: HearingTestSessionEntity)

    @Update
    suspend fun updateSession(session: HearingTestSessionEntity)

    @Query("DELETE FROM hearing_test_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("SELECT * FROM hearing_test_trials WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getTrialsForSession(sessionId: String): List<HearingTestTrialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrial(trial: HearingTestTrialEntity)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM hearing_profiles ORDER BY createdAt DESC")
    suspend fun getAllProfiles(): List<HearingProfileEntity>

    @Query("SELECT * FROM hearing_profiles WHERE id = :id")
    suspend fun getProfile(id: String): HearingProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: HearingProfileEntity)

    @Update
    suspend fun updateProfile(profile: HearingProfileEntity)

    @Query("DELETE FROM hearing_profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)

    @Query("UPDATE hearing_profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE hearing_profiles SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: String)

    @Query("UPDATE hearing_profiles SET thresholdCount = (SELECT COUNT(*) FROM hearing_thresholds WHERE profileId = :profileId) WHERE id = :profileId")
    suspend fun refreshThresholdCount(profileId: String)

    @Query("SELECT * FROM hearing_thresholds WHERE profileId = :profileId ORDER BY ear ASC, frequencyHz ASC")
    suspend fun getThresholds(profileId: String): List<HearingThresholdEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreshold(threshold: HearingThresholdEntity)
}

@Dao
interface DspPresetDao {
    @Query("SELECT * FROM dsp_presets ORDER BY createdAt DESC")
    suspend fun getAll(): List<DspPresetEntity>

    @Query("SELECT * FROM dsp_presets WHERE profileId = :profileId ORDER BY createdAt DESC")
    suspend fun getByProfileId(profileId: String): List<DspPresetEntity>

    @Query("SELECT * FROM dsp_presets WHERE id = :id")
    suspend fun getById(id: String): DspPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: DspPresetEntity)

    @Update
    suspend fun update(preset: DspPresetEntity)

    @Query("DELETE FROM dsp_presets WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE dsp_presets SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE dsp_presets SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: String)
}

@Dao
interface LiveSessionDao {
    @Query("SELECT * FROM live_sessions ORDER BY createdAt DESC")
    suspend fun getAll(): List<LiveSessionEntity>

    @Query("SELECT * FROM live_sessions WHERE id = :id")
    suspend fun getById(id: String): LiveSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: LiveSessionEntity)

    @Update
    suspend fun update(session: LiveSessionEntity)

    @Query("DELETE FROM live_sessions WHERE id = :id")
    suspend fun delete(id: String)
}
