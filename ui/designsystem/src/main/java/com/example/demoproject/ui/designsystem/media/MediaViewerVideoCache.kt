package com.example.demoproject.ui.designsystem.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Process-wide disk cache for MediaViewer video playback (Media3 [SimpleCache]).
 */
object MediaViewerVideoCache {
    private const val CACHE_DIR_NAME = "media_viewer_video"
    private const val MAX_CACHE_BYTES = 200L * 1024L * 1024L

    @Volatile
    private var simpleCache: SimpleCache? = null

    @OptIn(UnstableApi::class)
    fun get(context: Context): SimpleCache {
        val appContext = context.applicationContext
        return simpleCache ?: synchronized(this) {
            simpleCache ?: SimpleCache(
                File(appContext.cacheDir, CACHE_DIR_NAME),
                LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
                StandaloneDatabaseProvider(appContext),
            ).also { simpleCache = it }
        }
    }

    @OptIn(UnstableApi::class)
    fun createCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val upstream = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        return CacheDataSource.Factory()
            .setCache(get(context))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
