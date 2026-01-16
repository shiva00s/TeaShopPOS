package com.teashop.pos.di

import com.teashop.pos.data.MainRepository
import com.teashop.pos.sync.FirebaseSyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMainRepository(
        firebaseSync: FirebaseSyncManager
    ): MainRepository {
        return MainRepository(firebaseSync)
    }
}
