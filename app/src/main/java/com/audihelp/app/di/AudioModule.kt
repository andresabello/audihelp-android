package com.audihelp.app.di

import android.content.Context
import com.audihelp.app.audio.AudioEngineController
import com.audihelp.app.audio.UsbDeviceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides @Singleton
    fun provideAudioEngineController(): AudioEngineController = AudioEngineController()

    @Provides @Singleton
    fun provideUsbDeviceManager(@ApplicationContext context: Context): UsbDeviceManager =
        UsbDeviceManager(context)
}
