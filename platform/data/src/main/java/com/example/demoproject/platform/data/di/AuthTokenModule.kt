package com.example.demoproject.platform.data.di

import com.example.demoproject.platform.data.device.DefaultDeviceFingerprint
import com.example.demoproject.platform.data.locale.DefaultAppLocaleProvider
import com.example.demoproject.platform.data.session.SessionManager
import com.example.demoproject.platform.data.session.SessionUidProvider
import com.example.demoproject.platform.network.crypto.provider.DeviceFingerprint
import com.example.demoproject.platform.network.crypto.provider.UidProvider
import com.example.demoproject.platform.network.provider.AppLocaleProvider
import com.example.demoproject.platform.network.provider.AuthTokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthTokenModule {

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(sessionManager: SessionManager): AuthTokenProvider

    @Binds
    @Singleton
    abstract fun bindUidProvider(impl: SessionUidProvider): UidProvider

    @Binds
    @Singleton
    abstract fun bindDeviceFingerprint(impl: DefaultDeviceFingerprint): DeviceFingerprint

    @Binds
    @Singleton
    abstract fun bindAppLocaleProvider(impl: DefaultAppLocaleProvider): AppLocaleProvider
}
