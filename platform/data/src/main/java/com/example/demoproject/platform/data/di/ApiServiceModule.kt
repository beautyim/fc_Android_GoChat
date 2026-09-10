package com.example.demoproject.platform.data.di

import com.example.demoproject.platform.data.network.api.AppApi
import com.example.demoproject.platform.data.network.api.AuthApi
import com.example.demoproject.platform.data.network.api.CallApi
import com.example.demoproject.platform.data.network.api.FeedApi
import com.example.demoproject.platform.data.network.api.FirebaseApi
import com.example.demoproject.platform.data.network.api.GooglePayApi
import com.example.demoproject.platform.data.network.api.MatchApi
import com.example.demoproject.platform.data.network.api.MessageApi
import com.example.demoproject.platform.data.network.api.NotificationApi
import com.example.demoproject.platform.data.network.api.PostApi
import com.example.demoproject.platform.data.network.api.ReportApi
import com.example.demoproject.platform.data.network.api.ProfileApi
import com.example.demoproject.platform.data.network.api.TranslationApi
import com.example.demoproject.platform.data.network.api.CoinApi
import com.example.demoproject.platform.data.network.api.VipApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Provides Retrofit interfaces for non-auth APIs. Auth API lives in the auth feature
 * (`app/feature/auth/di/AuthModule`) to keep feature boundaries clean.
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiServiceModule {

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideAppApi(retrofit: Retrofit): AppApi = retrofit.create(AppApi::class.java)

    @Provides
    @Singleton
    fun provideFeedApi(retrofit: Retrofit): FeedApi = retrofit.create(FeedApi::class.java)

    @Provides
    @Singleton
    fun providePostApi(retrofit: Retrofit): PostApi = retrofit.create(PostApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideMessageApi(retrofit: Retrofit): MessageApi = retrofit.create(MessageApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi =
        retrofit.create(NotificationApi::class.java)

    @Provides
    @Singleton
    fun provideCallApi(retrofit: Retrofit): CallApi = retrofit.create(CallApi::class.java)

    @Provides
    @Singleton
    fun provideMatchApi(retrofit: Retrofit): MatchApi = retrofit.create(MatchApi::class.java)

    @Provides
    @Singleton
    fun provideReportApi(retrofit: Retrofit): ReportApi = retrofit.create(ReportApi::class.java)

    @Provides
    @Singleton
    fun provideVipApi(retrofit: Retrofit): VipApi = retrofit.create(VipApi::class.java)

    @Provides
    @Singleton
    fun provideCoinApi(retrofit: Retrofit): CoinApi = retrofit.create(CoinApi::class.java)

    @Provides
    @Singleton
    fun provideGooglePayApi(retrofit: Retrofit): GooglePayApi = retrofit.create(GooglePayApi::class.java)

    @Provides
    @Singleton
    fun provideTranslationApi(retrofit: Retrofit): TranslationApi =
        retrofit.create(TranslationApi::class.java)

    @Provides
    @Singleton
    fun provideFirebaseApi(retrofit: Retrofit): FirebaseApi = retrofit.create(FirebaseApi::class.java)
}
