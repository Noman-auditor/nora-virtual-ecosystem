package com.nora.ecosystem.di

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.UserManager
import androidx.room.Room
import com.nora.ecosystem.data.NoraDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides fun dpm(@ApplicationContext c: Context): DevicePolicyManager = c.getSystemService(DevicePolicyManager::class.java)
    @Provides fun um(@ApplicationContext c: Context): UserManager = c.getSystemService(UserManager::class.java)
    @Provides fun cm(@ApplicationContext c: Context): ConnectivityManager = c.getSystemService(ConnectivityManager::class.java)
    @Provides @Singleton fun db(@ApplicationContext c: Context): NoraDatabase =
        Room.databaseBuilder(c, NoraDatabase::class.java, "nora.db").build()
    @Provides fun spaceDao(db: NoraDatabase) = db.spaceDao()
    @Provides fun auditDao(db: NoraDatabase) = db.auditDao()
}
