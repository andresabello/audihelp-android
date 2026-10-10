package com.audihelp.app.di

import android.content.Context
import androidx.room.Room
import com.audihelp.app.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "audihelp.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideHearingTestDao(db: AppDatabase) = db.hearingTestDao()

    @Provides
    fun provideProfileDao(db: AppDatabase) = db.profileDao()

    @Provides
    fun provideDspPresetDao(db: AppDatabase) = db.dspPresetDao()

    @Provides
    fun provideLiveSessionDao(db: AppDatabase) = db.liveSessionDao()
}
