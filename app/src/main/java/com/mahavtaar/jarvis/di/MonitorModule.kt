package com.mahavtaar.jarvis.di

import android.content.Context
import com.mahavtaar.jarvis.domain.appcontrol.BatteryMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MonitorModule {
    @Provides
    @Singleton
    fun provideBatteryMonitor(@ApplicationContext context: Context): BatteryMonitor {
        return BatteryMonitor(context)
    }
}
