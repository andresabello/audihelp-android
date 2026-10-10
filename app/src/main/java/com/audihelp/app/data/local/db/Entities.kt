package com.audihelp.app.data.local.db

import androidx.room.*

@Entity(tableName = "hearing_test_sessions")
data class HearingTestSessionEntity(
    @PrimaryKey val id: String,
    val earTested: String,
    val status: String,
    val notes: String?,
    val startedAt: String?,
    val completedAt: String?,
    val createdAt: String,
)

@Entity(
    tableName = "hearing_test_trials",
    foreignKeys = [
        ForeignKey(
            entity = HearingTestSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("sessionId")],
)
data class HearingTestTrialEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val ear: String,
    val frequencyHz: Int,
    val levelDbfs: Float,
    val patternType: String,
    val response: String,
    val createdAt: String,
)

@Entity(tableName = "hearing_profiles")
data class HearingProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String?,
    val isActive: Boolean,
    val sourceSessionId: String?,
    val createdAt: String,
    val thresholdCount: Int = 0,
)

@Entity(
    tableName = "hearing_thresholds",
    foreignKeys = [
        ForeignKey(
            entity = HearingProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("profileId")],
)
data class HearingThresholdEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val ear: String,
    val frequencyHz: Int,
    val levelDbfs: Float,
    val response: String,
    val createdAt: String,
)

@Entity(
    tableName = "dsp_presets",
    foreignKeys = [
        ForeignKey(
            entity = HearingProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.SET_NULL,
        )
    ],
    indices = [Index("profileId")],
)
data class DspPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val configJson: String,
    val isActive: Boolean,
    val profileId: String?,
    val createdAt: String,
)

@Entity(tableName = "live_sessions")
data class LiveSessionEntity(
    @PrimaryKey val id: String,
    val presetId: String?,
    val profileId: String?,
    val notes: String?,
    val startedAt: String?,
    val endedAt: String?,
    val createdAt: String,
)
