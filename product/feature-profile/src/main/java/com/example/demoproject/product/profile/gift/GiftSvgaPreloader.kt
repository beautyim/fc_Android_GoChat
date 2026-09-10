package com.example.demoproject.product.profile.gift

import android.content.Context
import com.opensource.svgaplayer.SVGAParser
import com.opensource.svgaplayer.SVGAVideoEntity
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Prefetches gift SVGA so fullscreen play can start without waiting on the send round-trip.
 */
internal object GiftSvgaPreloader {
    private val cache = ConcurrentHashMap<String, SVGAVideoEntity>()
    private val loading = ConcurrentHashMap.newKeySet<String>()

    fun get(url: String): SVGAVideoEntity? = cache[url.trim()]

    fun put(url: String, entity: SVGAVideoEntity) {
        val key = url.trim()
        if (key.isNotBlank()) cache[key] = entity
    }

    fun prefetch(context: Context, url: String?) {
        val key = url?.trim().orEmpty()
        if (key.isBlank() || cache.containsKey(key) || !loading.add(key)) return
        val app = context.applicationContext
        val parser = SVGAParser(app)
        runCatching { URL(key) }
            .onSuccess { remote ->
                parser.decodeFromURL(
                    remote,
                    object : SVGAParser.ParseCompletion {
                        override fun onComplete(videoItem: SVGAVideoEntity) {
                            cache[key] = videoItem
                            loading.remove(key)
                        }

                        override fun onError() {
                            loading.remove(key)
                        }
                    },
                )
            }
            .onFailure { loading.remove(key) }
    }
}
