package com.audihelp.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        HearingTestSessionEntity::class,
        HearingTestTrialEntity::class,
        HearingProfileEntity::class,
        HearingThresholdEntity::class,
        DspPresetEntity::class,
        LiveSessionEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hearingTestDao(): HearingTestDao
    abstract fun profileDao(): ProfileDao
    abstract fun dspPresetDao(): DspPresetDao
    abstract fun liveSessionDao(): LiveSessionDao
}
